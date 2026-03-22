using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Account;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

/// <summary>
/// Handles registration, login, logout, password reset, and profile management.
///
/// FIASSE / SSEM controls applied:
///  - Passwords never appear in logs.
///  - Account enumeration is mitigated: ForgotPassword always returns the same view.
///  - Lockout is enforced by Identity (configured in Program.cs).
///  - Anti-forgery tokens required on all POST actions (global filter + [ValidateAntiForgeryToken]).
/// </summary>
public class AccountController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IEmailService _emailService;
    private readonly IAuditService _audit;
    private readonly ILogger<AccountController> _logger;

    public AccountController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IEmailService emailService,
        IAuditService audit,
        ILogger<AccountController> logger)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _emailService = emailService;
        _audit = audit;
        _logger = logger;
    }

    // -----------------------------------------------------------------------
    // Registration
    // -----------------------------------------------------------------------
    [HttpGet]
    public IActionResult Register() => View();

    [HttpPost]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = new ApplicationUser
        {
            UserName = model.UserName,
            Email = model.Email,
            DisplayName = model.UserName
        };

        var result = await _userManager.CreateAsync(user, model.Password);
        if (result.Succeeded)
        {
            await _userManager.AddToRoleAsync(user, Data.DbInitializer.UserRole);
            await _signInManager.SignInAsync(user, isPersistent: false);
            await _audit.LogAsync("UserRegistered", true, $"User {user.UserName} registered",
                user.Id, user.UserName, GetIp());
            _logger.LogInformation("New user registered: {UserName}", user.UserName);
            return RedirectToAction("Index", "Notes");
        }

        foreach (var err in result.Errors)
            ModelState.AddModelError(string.Empty, err.Description);

        return View(model);
    }

    // -----------------------------------------------------------------------
    // Login / Logout
    // -----------------------------------------------------------------------
    [HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        return View();
    }

    [HttpPost]
    public async Task<IActionResult> Login(LoginViewModel model, string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        if (!ModelState.IsValid) return View(model);

        var result = await _signInManager.PasswordSignInAsync(
            model.UserName, model.Password, model.RememberMe, lockoutOnFailure: true);

        if (result.Succeeded)
        {
            var user = await _userManager.FindByNameAsync(model.UserName)
                       ?? await _userManager.FindByEmailAsync(model.UserName);
            await _audit.LogAsync("LoginSuccess", true, null,
                user?.Id, model.UserName, GetIp());
            _logger.LogInformation("User {UserName} logged in", model.UserName);
            return LocalRedirect(IsLocalUrl(returnUrl) ? returnUrl! : "/");
        }

        if (result.IsLockedOut)
        {
            await _audit.LogAsync("AccountLockedOut", false, null, null, model.UserName, GetIp());
            _logger.LogWarning("Account locked out: {UserName}", model.UserName);
            return View("Lockout");
        }

        await _audit.LogAsync("LoginFailed", false, null, null, model.UserName, GetIp());
        // SSEM: Deliberately vague error to prevent username enumeration
        ModelState.AddModelError(string.Empty, "Invalid username or password.");
        return View(model);
    }

    [HttpPost]
    [Authorize]
    public async Task<IActionResult> Logout()
    {
        var userName = User.Identity?.Name;
        await _signInManager.SignOutAsync();
        await _audit.LogAsync("Logout", true, null, null, userName, GetIp());
        _logger.LogInformation("User {UserName} logged out", userName);
        return RedirectToAction("Index", "Home");
    }

    // -----------------------------------------------------------------------
    // Password Reset
    // -----------------------------------------------------------------------
    [HttpGet]
    public IActionResult ForgotPassword() => View();

    [HttpPost]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);
        // SSEM: Always return the same confirmation view to prevent account enumeration
        if (user != null)
        {
            var token = await _userManager.GeneratePasswordResetTokenAsync(user);
            var resetLink = Url.Action("ResetPassword", "Account",
                new { userId = user.Id, token = Uri.EscapeDataString(token) },
                protocol: Request.Scheme)!;

            await _emailService.SendPasswordResetAsync(user.Email!, resetLink);
            await _audit.LogAsync("PasswordResetRequested", true, null, user.Id, user.UserName, GetIp());
        }

        // Always redirect to confirmation – never disclose whether email exists
        return RedirectToAction(nameof(ForgotPasswordConfirmation));
    }

    [HttpGet]
    public IActionResult ForgotPasswordConfirmation() => View();

    [HttpGet]
    public IActionResult ResetPassword(string? userId, string? token)
    {
        if (userId is null || token is null) return BadRequest();
        return View(new ResetPasswordViewModel { UserId = userId, Token = Uri.UnescapeDataString(token) });
    }

    [HttpPost]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByIdAsync(model.UserId);
        if (user is null)
        {
            // SSEM: Prevent user-ID enumeration – show generic error
            ModelState.AddModelError(string.Empty, "Password reset failed. The link may be expired or invalid.");
            return View(model);
        }

        var result = await _userManager.ResetPasswordAsync(user, model.Token, model.NewPassword);
        if (result.Succeeded)
        {
            await _audit.LogAsync("PasswordReset", true, null, user.Id, user.UserName, GetIp());
            return RedirectToAction(nameof(ResetPasswordConfirmation));
        }

        await _audit.LogAsync("PasswordResetFailed", false, null, user.Id, user.UserName, GetIp());
        foreach (var err in result.Errors)
            ModelState.AddModelError(string.Empty, err.Description);

        return View(model);
    }

    [HttpGet]
    public IActionResult ResetPasswordConfirmation() => View();

    // -----------------------------------------------------------------------
    // Profile
    // -----------------------------------------------------------------------
    [HttpGet, Authorize]
    public async Task<IActionResult> Profile()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is null) return Challenge();

        return View(new ProfileViewModel
        {
            UserName = user.UserName ?? string.Empty,
            Email = user.Email ?? string.Empty
        });
    }

    [HttpPost, Authorize]
    public async Task<IActionResult> Profile(ProfileViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.GetUserAsync(User);
        if (user is null) return Challenge();

        // Update username
        if (user.UserName != model.UserName)
        {
            var setUser = await _userManager.SetUserNameAsync(user, model.UserName);
            if (!setUser.Succeeded)
            {
                foreach (var e in setUser.Errors) ModelState.AddModelError(string.Empty, e.Description);
                return View(model);
            }
        }

        // Update email
        if (user.Email != model.Email)
        {
            var setEmail = await _userManager.SetEmailAsync(user, model.Email);
            if (!setEmail.Succeeded)
            {
                foreach (var e in setEmail.Errors) ModelState.AddModelError(string.Empty, e.Description);
                return View(model);
            }
        }

        // Update password (only if fields provided)
        if (!string.IsNullOrEmpty(model.NewPassword))
        {
            if (string.IsNullOrEmpty(model.CurrentPassword))
            {
                ModelState.AddModelError(nameof(model.CurrentPassword), "Current password is required to set a new password.");
                return View(model);
            }

            var changePwd = await _userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);
            if (!changePwd.Succeeded)
            {
                foreach (var e in changePwd.Errors) ModelState.AddModelError(string.Empty, e.Description);
                return View(model);
            }

            await _signInManager.RefreshSignInAsync(user);
            await _audit.LogAsync("PasswordChanged", true, null, user.Id, user.UserName, GetIp());
        }

        await _audit.LogAsync("ProfileUpdated", true, null, user.Id, user.UserName, GetIp());
        TempData["Success"] = "Profile updated successfully.";
        return RedirectToAction(nameof(Profile));
    }

    [HttpGet]
    public IActionResult AccessDenied() => View();

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private string? GetIp() =>
        HttpContext.Connection.RemoteIpAddress?.ToString();

    /// <summary>Guard against open redirect – only allow local URLs.</summary>
    private bool IsLocalUrl(string? url) =>
        !string.IsNullOrEmpty(url) && Url.IsLocalUrl(url);
}
