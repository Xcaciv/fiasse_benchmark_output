using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Account;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;

namespace LooseNotes.Controllers;

/// <summary>
/// Handles authentication flows: register, login, logout, password reset.
/// Trust boundary: all user-supplied input is validated via model binding + Identity.
/// Rate limiting applied at login and register (Availability).
/// </summary>
[AllowAnonymous]
public class AccountController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IEmailService _emailService;
    private readonly IAuditService _auditService;
    private readonly ILogger<AccountController> _logger;

    public AccountController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IEmailService emailService,
        IAuditService auditService,
        ILogger<AccountController> logger)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _emailService = emailService;
        _auditService = auditService;
        _logger = logger;
    }

    // ── Register ─────────────────────────────────────────────────────────────

    [HttpGet]
    public IActionResult Register() => View();

    [HttpPost, ValidateAntiForgeryToken]
    [EnableRateLimiting("register")]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        if (!ModelState.IsValid)
            return View(model);

        var user = new ApplicationUser
        {
            UserName = model.Email,
            Email = model.Email,
            DisplayName = model.DisplayName,
            CreatedAt = DateTime.UtcNow
        };

        var result = await _userManager.CreateAsync(user, model.Password);
        if (!result.Succeeded)
        {
            AddIdentityErrors(result);
            await _auditService.RecordAsync("Register", succeeded: false,
                metadataJson: $"{{\"email\":\"{SanitizeForLog(model.Email)}\"}}",
                ipAddress: GetClientIp());
            return View(model);
        }

        await _userManager.AddToRoleAsync(user, Data.DbInitializer.UserRoleName);
        await _auditService.RecordAsync("Register", userId: user.Id, succeeded: true,
            metadataJson: $"{{\"email\":\"{SanitizeForLog(model.Email)}\"}}",
            ipAddress: GetClientIp());

        await _signInManager.SignInAsync(user, isPersistent: false);
        return RedirectToAction("Index", "Notes");
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    [HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        return View(new LoginViewModel { ReturnUrl = returnUrl });
    }

    [HttpPost, ValidateAntiForgeryToken]
    [EnableRateLimiting("login")]
    public async Task<IActionResult> Login(LoginViewModel model)
    {
        if (!ModelState.IsValid)
            return View(model);

        var result = await _signInManager.PasswordSignInAsync(
            model.Email, model.Password, model.RememberMe, lockoutOnFailure: true);

        if (result.Succeeded)
        {
            var user = await _userManager.FindByEmailAsync(model.Email);
            await _auditService.RecordAsync("Login", userId: user?.Id, succeeded: true,
                ipAddress: GetClientIp());

            return RedirectToSafeUrl(model.ReturnUrl);
        }

        if (result.IsLockedOut)
        {
            await _auditService.RecordAsync("LoginLockedOut", succeeded: false,
                metadataJson: $"{{\"email\":\"{SanitizeForLog(model.Email)}\"}}",
                ipAddress: GetClientIp());
            return RedirectToAction(nameof(Lockout));
        }

        // Use generic error — do not reveal whether email exists (Confidentiality)
        ModelState.AddModelError(string.Empty, "Invalid login attempt.");
        await _auditService.RecordAsync("LoginFailed", succeeded: false,
            metadataJson: $"{{\"email\":\"{SanitizeForLog(model.Email)}\"}}",
            ipAddress: GetClientIp());
        return View(model);
    }

    [HttpPost, ValidateAntiForgeryToken]
    [Authorize]
    public async Task<IActionResult> Logout()
    {
        var userId = _userManager.GetUserId(User);
        await _signInManager.SignOutAsync();
        await _auditService.RecordAsync("Logout", userId: userId, ipAddress: GetClientIp());
        return RedirectToAction("Index", "Home");
    }

    [HttpGet]
    public IActionResult Lockout() => View();

    // ── Password Reset ────────────────────────────────────────────────────────

    [HttpGet]
    public IActionResult ForgotPassword() => View();

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model)
    {
        if (!ModelState.IsValid)
            return View(model);

        // Always show confirmation to prevent email enumeration (Confidentiality)
        var user = await _userManager.FindByEmailAsync(model.Email);
        if (user is not null && await _userManager.IsEmailConfirmedAsync(user))
        {
            var token = await _userManager.GeneratePasswordResetTokenAsync(user);
            var resetLink = Url.Action(nameof(ResetPassword), "Account",
                new { token, email = model.Email }, protocol: Request.Scheme)!;

            await _emailService.SendPasswordResetAsync(model.Email, resetLink);
            await _auditService.RecordAsync("PasswordResetRequested", userId: user.Id,
                ipAddress: GetClientIp());
        }

        return RedirectToAction(nameof(ForgotPasswordConfirmation));
    }

    [HttpGet]
    public IActionResult ForgotPasswordConfirmation() => View();

    [HttpGet]
    public IActionResult ResetPassword(string? token, string? email)
    {
        if (token is null || email is null)
            return BadRequest("Invalid password reset link.");

        return View(new ResetPasswordViewModel { Token = token, Email = email });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model)
    {
        if (!ModelState.IsValid)
            return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);
        // Generic message prevents email enumeration (Confidentiality)
        if (user is null)
            return RedirectToAction(nameof(ResetPasswordConfirmation));

        var result = await _userManager.ResetPasswordAsync(user, model.Token, model.Password);
        if (result.Succeeded)
        {
            await _auditService.RecordAsync("PasswordReset", userId: user.Id,
                succeeded: true, ipAddress: GetClientIp());
            return RedirectToAction(nameof(ResetPasswordConfirmation));
        }

        AddIdentityErrors(result);
        await _auditService.RecordAsync("PasswordResetFailed", userId: user.Id,
            succeeded: false, ipAddress: GetClientIp());
        return View(model);
    }

    [HttpGet]
    public IActionResult ResetPasswordConfirmation() => View();

    [HttpGet]
    public IActionResult AccessDenied() => View();

    // ── Private helpers ───────────────────────────────────────────────────────

    private void AddIdentityErrors(IdentityResult result)
    {
        foreach (var error in result.Errors)
            ModelState.AddModelError(string.Empty, error.Description);
    }

    /// <summary>
    /// Validates returnUrl is local before redirecting (prevents open redirect).
    /// Only local URLs are accepted (Integrity).
    /// </summary>
    private IActionResult RedirectToSafeUrl(string? returnUrl)
    {
        if (!string.IsNullOrEmpty(returnUrl) && Url.IsLocalUrl(returnUrl))
            return LocalRedirect(returnUrl);

        return RedirectToAction("Index", "Notes");
    }

    /// <summary>
    /// Strips characters that could break structured log formats.
    /// Email addresses are loggable; tokens/passwords must never be passed here.
    /// </summary>
    private static string SanitizeForLog(string value)
        => value.Replace("\"", "").Replace("\n", "").Replace("\r", "")[..Math.Min(value.Length, 256)];

    private string GetClientIp()
        => HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";
}
