using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels;

namespace LooseNotes.Controllers;

public class AccountController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IEmailService _emailService;
    private readonly ILogger<AccountController> _logger;

    public AccountController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IEmailService emailService,
        ILogger<AccountController> logger)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _emailService = emailService;
        _logger = logger;
    }

    [HttpGet]
    public IActionResult Register() => View();

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = new ApplicationUser { UserName = model.Username, Email = model.Email, EmailConfirmed = true };
        var result = await _userManager.CreateAsync(user, model.Password);
        if (result.Succeeded)
        {
            await _userManager.AddToRoleAsync(user, "User");
            await _signInManager.SignInAsync(user, isPersistent: false);
            _logger.LogInformation("User {Username} registered.", model.Username);
            return RedirectToAction("Index", "Notes");
        }
        foreach (var error in result.Errors)
            ModelState.AddModelError(string.Empty, error.Description);
        return View(model);
    }

    [HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        return View();
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Login(LoginViewModel model, string? returnUrl = null)
    {
        if (!ModelState.IsValid) return View(model);

        var result = await _signInManager.PasswordSignInAsync(model.Username, model.Password, model.RememberMe, lockoutOnFailure: false);
        if (result.Succeeded)
        {
            _logger.LogInformation("User {Username} logged in.", model.Username);
            if (!string.IsNullOrEmpty(returnUrl) && Url.IsLocalUrl(returnUrl))
                return Redirect(returnUrl);
            return RedirectToAction("Index", "Notes");
        }
        _logger.LogWarning("Failed login attempt for {Username}.", model.Username);
        ModelState.AddModelError(string.Empty, "Invalid username or password.");
        return View(model);
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Logout()
    {
        var name = User.Identity?.Name;
        await _signInManager.SignOutAsync();
        _logger.LogInformation("User {Username} logged out.", name);
        return RedirectToAction("Index", "Home");
    }

    [HttpGet]
    public IActionResult ForgotPassword() => View();

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);
        if (user != null)
        {
            var token = await _userManager.GeneratePasswordResetTokenAsync(user);
            var resetLink = Url.Action("ResetPassword", "Account",
                new { token, email = model.Email }, Request.Scheme)!;
            await _emailService.SendEmailAsync(model.Email, "Reset your password",
                $"Reset your password: {resetLink}");
        }
        return RedirectToAction(nameof(ForgotPasswordConfirmation));
    }

    [HttpGet]
    public IActionResult ForgotPasswordConfirmation() => View();

    [HttpGet]
    public IActionResult ResetPassword(string token, string email) =>
        View(new ResetPasswordViewModel { Token = token, Email = email });

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);
        if (user == null) return RedirectToAction(nameof(ResetPasswordConfirmation));

        var result = await _userManager.ResetPasswordAsync(user, model.Token, model.Password);
        if (result.Succeeded)
            return RedirectToAction(nameof(ResetPasswordConfirmation));

        foreach (var error in result.Errors)
            ModelState.AddModelError(string.Empty, error.Description);
        return View(model);
    }

    [HttpGet]
    public IActionResult ResetPasswordConfirmation() => View();

    [HttpGet]
    public IActionResult AccessDenied() => View();
}
