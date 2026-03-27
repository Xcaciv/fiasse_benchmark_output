using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Account;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace LooseNotes.Controllers;

/// <summary>
/// Handles user registration, login, logout, and password reset.
/// Authentication logic is centralized here; not duplicated across controllers (Analyzability).
/// All session mutations (login, logout, reset) are logged as security events (Accountability).
/// </summary>
[AutoValidateAntiforgeryToken]
public sealed class AccountController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IEmailService _emailService;
    private readonly IAuditService _auditService;
    private readonly IPasswordValidationService _passwordValidation;
    private readonly ILogger<AccountController> _logger;

    public AccountController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IEmailService emailService,
        IAuditService auditService,
        IPasswordValidationService passwordValidation,
        ILogger<AccountController> logger)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _emailService = emailService;
        _auditService = auditService;
        _passwordValidation = passwordValidation;
        _logger = logger;
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult Login(string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        return View(new LoginViewModel { ReturnUrl = returnUrl });
    }

    [HttpPost]
    [AllowAnonymous]
    [EnableRateLimiting("login")]
    public async Task<IActionResult> Login(LoginViewModel model)
    {
        var sourceIp = GetClientIp();

        if (!ModelState.IsValid)
            return View(model);

        // Attempt sign-in. SignInManager handles session fixation prevention by
        // ASP.NET Core Identity's default behaviour (new session on authentication).
        var result = await _signInManager.PasswordSignInAsync(
            model.Username, model.Password, isPersistent: false, lockoutOnFailure: true);

        if (result.Succeeded)
        {
            var user = await _userManager.FindByNameAsync(model.Username);
            if (user != null)
            {
                user.LastLoginAt = DateTime.UtcNow;
                await _userManager.UpdateAsync(user);
            }

            await _auditService.LogAsync(
                AuditEventTypes.UserLoginSuccess,
                user?.Id, model.Username, sourceIp,
                outcome: "success");

            _logger.LogInformation("User {Username} logged in from {IP}", model.Username, sourceIp);

            return RedirectToLocal(model.ReturnUrl);
        }

        if (result.IsLockedOut)
        {
            await _auditService.LogAsync(
                AuditEventTypes.UserLoginFailed,
                null, model.Username, sourceIp,
                outcome: "locked_out");

            // Enumeration-safe: same error message as wrong password (ASVS V2.2.1)
            ModelState.AddModelError(string.Empty, "Invalid username or password.");
            return View(model);
        }

        // Enumeration-safe: identical message for unknown user and wrong password
        await _auditService.LogAsync(
            AuditEventTypes.UserLoginFailed,
            null, model.Username, sourceIp,
            outcome: "failed");

        ModelState.AddModelError(string.Empty, "Invalid username or password.");
        return View(model);
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult Register()
    {
        return View(new RegisterViewModel());
    }

    [HttpPost]
    [AllowAnonymous]
    [EnableRateLimiting("registration")]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        var sourceIp = GetClientIp();

        if (!ModelState.IsValid)
            return View(model);

        // Server-side password policy validation (ASVS V6.2.1, V6.2.4)
        var passwordError = _passwordValidation.Validate(model.Password);
        if (passwordError != null)
        {
            ModelState.AddModelError(nameof(model.Password), passwordError);
            return View(model);
        }

        var user = new ApplicationUser
        {
            UserName = model.Username,
            Email = model.Email,
            DisplayName = model.Username,
            CreatedAt = DateTime.UtcNow
        };

        var result = await _userManager.CreateAsync(user, model.Password);

        if (result.Succeeded)
        {
            // Sign in after registration - Identity generates new session token (ASVS V7.2.4)
            await _signInManager.SignInAsync(user, isPersistent: false);

            await _auditService.LogAsync(
                AuditEventTypes.UserRegistered,
                user.Id, model.Username, sourceIp,
                outcome: "success",
                details: $"email_domain:{GetEmailDomain(model.Email)}");

            _logger.LogInformation("User {Username} registered from {IP}", model.Username, sourceIp);
            return RedirectToAction("Index", "Notes");
        }

        // Enumeration-safe: do not reveal whether username/email already exists
        // Map all identity errors to a generic message
        var hasConflict = result.Errors.Any(e =>
            e.Code is "DuplicateUserName" or "DuplicateEmail");

        if (hasConflict)
        {
            await _auditService.LogAsync(
                AuditEventTypes.UserRegistered,
                null, model.Username, sourceIp,
                outcome: "duplicate",
                details: "duplicate_registration_attempt");
        }

        // Same error message regardless of reason - enumeration-safe (ASVS V6.2.8)
        ModelState.AddModelError(string.Empty,
            "Registration could not be completed. Please check your details and try again.");
        return View(model);
    }

    [HttpPost]
    [Authorize]
    public async Task<IActionResult> Logout()
    {
        var userId = _userManager.GetUserId(User);
        var username = _userManager.GetUserName(User);
        var sourceIp = GetClientIp();

        await _signInManager.SignOutAsync();

        await _auditService.LogAsync(
            AuditEventTypes.UserLogout,
            userId, username, sourceIp);

        return RedirectToAction("Index", "Home");
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult ForgotPassword()
    {
        return View(new ForgotPasswordViewModel());
    }

    [HttpPost]
    [AllowAnonymous]
    [EnableRateLimiting("passwordReset")]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model)
    {
        var sourceIp = GetClientIp();

        if (!ModelState.IsValid)
            // Still show confirmation - enumeration-safe
            return View("ForgotPasswordConfirmation");

        var user = await _userManager.FindByEmailAsync(model.Email);

        // Always log and always show confirmation - never reveal email existence (ASVS V6.2.3)
        await _auditService.LogAsync(
            AuditEventTypes.PasswordResetRequested,
            user?.Id, user?.UserName, sourceIp,
            outcome: user != null ? "token_generated" : "email_not_found",
            details: $"email_domain:{GetEmailDomain(model.Email)}");

        if (user != null)
        {
            // Identity generates CSPRNG token with >128 bits entropy (ASVS V7.2.3)
            var token = await _userManager.GeneratePasswordResetTokenAsync(user);
            var resetLink = Url.Action("ResetPassword", "Account",
                new { token, email = model.Email }, Request.Scheme)!;

            await _emailService.SendPasswordResetAsync(model.Email, resetLink);
        }

        // Identical response regardless of email existence (ASVS V6.2.3)
        return View("ForgotPasswordConfirmation");
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult ForgotPasswordConfirmation() => View();

    [HttpGet]
    [AllowAnonymous]
    public IActionResult ResetPassword(string? token, string? email)
    {
        if (string.IsNullOrEmpty(token) || string.IsNullOrEmpty(email))
            return BadRequest("Invalid password reset link.");

        return View(new ResetPasswordViewModel { Token = token, Email = email });
    }

    [HttpPost]
    [AllowAnonymous]
    [EnableRateLimiting("passwordReset")]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model)
    {
        var sourceIp = GetClientIp();

        if (!ModelState.IsValid)
            return View(model);

        var passwordError = _passwordValidation.Validate(model.NewPassword);
        if (passwordError != null)
        {
            ModelState.AddModelError(nameof(model.NewPassword), passwordError);
            return View(model);
        }

        var user = await _userManager.FindByEmailAsync(model.Email);
        if (user == null)
        {
            // Do not reveal the email doesn't exist
            return RedirectToAction("ResetPasswordConfirmation");
        }

        var result = await _userManager.ResetPasswordAsync(user, model.Token, model.NewPassword);

        if (result.Succeeded)
        {
            // Terminate all active sessions after password reset (ASVS V7.4.3)
            await _userManager.UpdateSecurityStampAsync(user);

            await _auditService.LogAsync(
                AuditEventTypes.PasswordResetCompleted,
                user.Id, user.UserName, sourceIp,
                outcome: "success");

            _logger.LogInformation("Password reset completed for user {UserId}", user.Id);
            return RedirectToAction("ResetPasswordConfirmation");
        }

        // Token reuse or expiry
        var isExpiredOrReused = result.Errors.Any(e => e.Code == "InvalidToken");
        if (isExpiredOrReused)
        {
            await _auditService.LogAsync(
                AuditEventTypes.PasswordResetTokenReused,
                user.Id, user.UserName, sourceIp,
                outcome: "token_invalid");
        }

        ModelState.AddModelError(string.Empty, "Password reset failed. The link may have expired or already been used.");
        return View(model);
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult ResetPasswordConfirmation() => View();

    [HttpGet]
    public IActionResult AccessDenied() => View();

    // Whitelist-based return URL validation - never redirect to external URLs (Open Redirect prevention)
    private IActionResult RedirectToLocal(string? returnUrl)
    {
        if (!string.IsNullOrEmpty(returnUrl) && Url.IsLocalUrl(returnUrl))
            return Redirect(returnUrl);
        return RedirectToAction("Index", "Notes");
    }

    private string GetClientIp()
    {
        return HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";
    }

    private static string GetEmailDomain(string email)
    {
        var atIndex = email.IndexOf('@');
        return atIndex >= 0 ? email[(atIndex + 1)..] : "unknown";
    }
}
