using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Account;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

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

    [HttpGet]
    public IActionResult Register() => View();

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Register([Bind("Username,Email,Password,ConfirmPassword")] RegisterViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = new ApplicationUser
        {
            UserName = model.Username,
            Email = model.Email,
            DisplayName = model.Username,
            CreatedAt = DateTime.UtcNow
        };

        var result = await _userManager.CreateAsync(user, model.Password);

        if (!result.Succeeded)
        {
            foreach (var error in result.Errors)
            {
                ModelState.AddModelError(string.Empty, error.Description);
            }
            _audit.LogAuthEvent("REGISTER_FAILED", "N/A", $"Username: {model.Username}");
            return View(model);
        }

        _audit.LogAuthEvent("REGISTER_SUCCESS", user.Id, $"Username: {model.Username}");
        await _emailService.SendWelcomeEmailAsync(model.Email, model.Username);
        await _signInManager.SignInAsync(user, isPersistent: false);
        return RedirectToAction("Index", "Notes");
    }

    [HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        return View();
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Login([Bind("Email,Password,RememberMe")] LoginViewModel model, string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;

        if (!ModelState.IsValid) return View(model);

        var result = await _signInManager.PasswordSignInAsync(
            model.Email, model.Password, model.RememberMe, lockoutOnFailure: true);

        if (result.Succeeded)
        {
            var user = await _userManager.FindByEmailAsync(model.Email);
            _audit.LogAuthEvent("LOGIN_SUCCESS", user?.Id ?? "unknown", "Login via email");
            return RedirectToLocal(returnUrl);
        }

        if (result.IsLockedOut)
        {
            _audit.LogAuthEvent("LOGIN_LOCKOUT", "unknown", $"Email: {model.Email}");
            return View("Lockout");
        }

        ModelState.AddModelError(string.Empty, "Invalid login attempt.");
        _audit.LogAuthEvent("LOGIN_FAILED", "unknown", "Invalid credentials");
        return View(model);
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Logout()
    {
        var userId = _userManager.GetUserId(User) ?? "unknown";
        await _signInManager.SignOutAsync();
        _audit.LogAuthEvent("LOGOUT", userId, "User signed out");
        return RedirectToAction("Index", "Home");
    }

    [HttpGet]
    public IActionResult ForgotPassword() => View();

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ForgotPassword([Bind("Email")] ForgotPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);
        // Always return same response to prevent user enumeration
        if (user != null)
        {
            var token = await _userManager.GeneratePasswordResetTokenAsync(user);
            var resetLink = Url.Action("ResetPassword", "Account",
                new { email = model.Email, token }, Request.Scheme)!;
            await _emailService.SendPasswordResetEmailAsync(model.Email, resetLink);
            _audit.LogAuthEvent("PASSWORD_RESET_REQUESTED", user.Id, "Reset token generated");
        }

        return RedirectToAction(nameof(ForgotPasswordConfirmation));
    }

    [HttpGet]
    public IActionResult ForgotPasswordConfirmation() => View();

    [HttpGet]
    public IActionResult ResetPassword(string? email, string? token)
    {
        if (string.IsNullOrWhiteSpace(email) || string.IsNullOrWhiteSpace(token))
        {
            return BadRequest("Invalid reset link.");
        }
        return View(new ResetPasswordViewModel { Email = email, Token = token });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ResetPassword([Bind("Email,Token,NewPassword,ConfirmNewPassword")] ResetPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);
        if (user is null)
        {
            return RedirectToAction(nameof(ResetPasswordConfirmation));
        }

        var result = await _userManager.ResetPasswordAsync(user, model.Token, model.NewPassword);
        if (!result.Succeeded)
        {
            foreach (var error in result.Errors)
            {
                ModelState.AddModelError(string.Empty, error.Description);
            }
            return View(model);
        }

        _audit.LogAuthEvent("PASSWORD_RESET_SUCCESS", user.Id, "Password reset completed");
        return RedirectToAction(nameof(ResetPasswordConfirmation));
    }

    [HttpGet]
    public IActionResult ResetPasswordConfirmation() => View();

    private IActionResult RedirectToLocal(string? returnUrl)
    {
        if (!string.IsNullOrEmpty(returnUrl) && Url.IsLocalUrl(returnUrl))
        {
            return Redirect(returnUrl);
        }
        return RedirectToAction("Index", "Notes");
    }
}
