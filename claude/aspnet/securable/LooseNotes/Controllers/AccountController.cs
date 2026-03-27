// AccountController.cs — Authentication and account management.
// Trust boundary: ALL user input is validated via ModelState before processing.
// Authenticity: uses ASP.NET Core Identity (PBKDF2 hashing, lockout, token validation).
// Accountability: all auth events are audited.
// Confidentiality: errors are generic — do not reveal whether an email exists.
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Account;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

public sealed class AccountController : Controller
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

    // ── GET /Account/Register ─────────────────────────────────────────────────
    [HttpGet, AllowAnonymous]
    public IActionResult Register() => View();

    // ── POST /Account/Register ────────────────────────────────────────────────
    [HttpPost, AllowAnonymous]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        // Trust boundary: reject invalid input before touching Identity
        if (!ModelState.IsValid) return View(model);

        var user = new ApplicationUser
        {
            UserName = model.UserName,
            Email = model.Email
        };

        // Authenticity: Identity hashes the password with PBKDF2
        var result = await _userManager.CreateAsync(user, model.Password);

        if (result.Succeeded)
        {
            await _userManager.AddToRoleAsync(user, "User");
            await _auditService.LogAsync(user.Id, "User.Registered", "User", user.Id,
                $"Username={user.UserName}");

            await _signInManager.SignInAsync(user, isPersistent: false);
            return RedirectToAction("Index", "Notes");
        }

        // Resilience: surface only safe error descriptions (Identity codes, not internals)
        foreach (var error in result.Errors)
            ModelState.AddModelError(string.Empty, error.Description);

        return View(model);
    }

    // ── GET /Account/Login ────────────────────────────────────────────────────
    [HttpGet, AllowAnonymous]
    public IActionResult Login(string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        return View();
    }

    // ── POST /Account/Login ───────────────────────────────────────────────────
    [HttpPost, AllowAnonymous]
    public async Task<IActionResult> Login(LoginViewModel model, string? returnUrl = null)
    {
        if (!ModelState.IsValid) return View(model);

        // Authenticity: lockoutOnFailure prevents brute force
        var result = await _signInManager.PasswordSignInAsync(
            model.UserName, model.Password, model.RememberMe, lockoutOnFailure: true);

        if (result.Succeeded)
        {
            var user = await _userManager.FindByNameAsync(model.UserName);
            await _auditService.LogAsync(user?.Id, "User.Login", "User", user?.Id);
            _logger.LogInformation("User {User} logged in", model.UserName);
            return RedirectToSafeUrl(returnUrl);
        }

        if (result.IsLockedOut)
        {
            await _auditService.LogAsync(null, "User.Login.LockedOut", "User",
                details: $"Username={model.UserName}");
            return RedirectToAction(nameof(Lockout));
        }

        // Confidentiality: generic error — do not reveal which field was wrong
        await _auditService.LogAsync(null, "User.Login.Failed", details: $"Username={model.UserName}");
        ModelState.AddModelError(string.Empty, "Invalid username or password.");
        return View(model);
    }

    // ── POST /Account/Logout ──────────────────────────────────────────────────
    [HttpPost, Authorize]
    public async Task<IActionResult> Logout()
    {
        var userId = _userManager.GetUserId(User);
        await _signInManager.SignOutAsync();
        await _auditService.LogAsync(userId, "User.Logout");
        return RedirectToAction("Index", "Home");
    }

    // ── GET /Account/Lockout ──────────────────────────────────────────────────
    [HttpGet, AllowAnonymous]
    public IActionResult Lockout() => View();

    // ── GET /Account/AccessDenied ─────────────────────────────────────────────
    [HttpGet, AllowAnonymous]
    public IActionResult AccessDenied() => View();

    // ── GET /Account/ForgotPassword ───────────────────────────────────────────
    [HttpGet, AllowAnonymous]
    public IActionResult ForgotPassword() => View();

    // ── POST /Account/ForgotPassword ──────────────────────────────────────────
    [HttpPost, AllowAnonymous]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);

        // Confidentiality + Availability: always redirect, whether user exists or not
        // (prevents email enumeration)
        if (user is null)
            return RedirectToAction(nameof(ForgotPasswordConfirmation));

        var token = await _userManager.GeneratePasswordResetTokenAsync(user);
        var resetLink = Url.Action(
            nameof(ResetPassword),
            "Account",
            new { token, email = model.Email },
            protocol: Request.Scheme)!;

        await _emailService.SendPasswordResetAsync(model.Email, resetLink);
        await _auditService.LogAsync(user.Id, "User.PasswordResetRequested", "User", user.Id);

        return RedirectToAction(nameof(ForgotPasswordConfirmation));
    }

    // ── GET /Account/ForgotPasswordConfirmation ───────────────────────────────
    [HttpGet, AllowAnonymous]
    public IActionResult ForgotPasswordConfirmation() => View();

    // ── GET /Account/ResetPassword ────────────────────────────────────────────
    [HttpGet, AllowAnonymous]
    public IActionResult ResetPassword(string? token, string? email)
    {
        if (token is null || email is null)
            return BadRequest("Invalid password reset link.");

        var model = new ResetPasswordViewModel { Token = token, Email = email };
        return View(model);
    }

    // ── POST /Account/ResetPassword ───────────────────────────────────────────
    [HttpPost, AllowAnonymous]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);

        // Confidentiality: redirect even if user not found — no enumeration
        if (user is null)
            return RedirectToAction(nameof(ResetPasswordConfirmation));

        // Authenticity: Identity validates token expiry and integrity
        var result = await _userManager.ResetPasswordAsync(user, model.Token, model.Password);

        if (result.Succeeded)
        {
            await _auditService.LogAsync(user.Id, "User.PasswordReset", "User", user.Id);
            return RedirectToAction(nameof(ResetPasswordConfirmation));
        }

        foreach (var error in result.Errors)
            ModelState.AddModelError(string.Empty, error.Description);

        return View(model);
    }

    // ── GET /Account/ResetPasswordConfirmation ────────────────────────────────
    [HttpGet, AllowAnonymous]
    public IActionResult ResetPasswordConfirmation() => View();

    // ── Private helpers ───────────────────────────────────────────────────────

    /// <summary>Redirect to returnUrl only if it is a local URL (prevents open redirect).</summary>
    private IActionResult RedirectToSafeUrl(string? returnUrl)
    {
        if (!string.IsNullOrEmpty(returnUrl) && Url.IsLocalUrl(returnUrl))
            return LocalRedirect(returnUrl);

        return RedirectToAction("Index", "Notes");
    }
}
