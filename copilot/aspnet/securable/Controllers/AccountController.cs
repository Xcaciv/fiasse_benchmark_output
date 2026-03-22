using LooseNotes.Models;
using LooseNotes.Services.Interfaces;
using LooseNotes.ViewModels.Account;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

/// <summary>
/// Trust boundary: all incoming auth data validated before processing.
/// Passwords and tokens never appear in log messages (Confidentiality).
/// </summary>
[Route("[controller]/[action]")]
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

    [HttpGet]
    [AllowAnonymous]
    public IActionResult Register() => View();

    [HttpPost]
    [AllowAnonymous]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        // Trust boundary entry point — model state validated first (Integrity)
        _logger.LogInformation("Register attempt for username {Username}", model.UserName);

        if (!ModelState.IsValid) return View(model);

        var user = new ApplicationUser { UserName = model.UserName, Email = model.Email };
        var result = await _userManager.CreateAsync(user, model.Password);

        if (!result.Succeeded)
        {
            AddIdentityErrors(result);
            return View(model);
        }

        await _userManager.AddToRoleAsync(user, "User");
        await _auditService.LogAsync("UserRegistered", user.Id, $"Username={user.UserName}", GetClientIp());
        await _signInManager.SignInAsync(user, isPersistent: false);

        return RedirectToAction("Index", "Notes");
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult Login(string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        return View();
    }

    [HttpPost]
    [AllowAnonymous]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Login(LoginViewModel model, string? returnUrl = null)
    {
        // Trust boundary — validate before processing credentials
        _logger.LogInformation("Login attempt for {Username}", model.UserName);

        if (!ModelState.IsValid) return View(model);

        var result = await _signInManager.PasswordSignInAsync(
            model.UserName, model.Password, model.RememberMe, lockoutOnFailure: true);

        if (result.Succeeded)
        {
            var user = await _userManager.FindByNameAsync(model.UserName);
            await _auditService.LogAsync("UserLogin", user?.Id, $"Username={model.UserName}", GetClientIp());
            return RedirectToLocal(returnUrl);
        }

        if (result.IsLockedOut)
        {
            _logger.LogWarning("Account locked out for {Username}", model.UserName);
            return RedirectToAction(nameof(Lockout));
        }

        // Generic error — do not reveal whether username or password was wrong (Confidentiality)
        ModelState.AddModelError(string.Empty, "Invalid login attempt.");
        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Logout()
    {
        var userId = _userManager.GetUserId(User);
        await _signInManager.SignOutAsync();
        await _auditService.LogAsync("UserLogout", userId, null, GetClientIp());
        return RedirectToAction("Index", "Home");
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult Lockout() => View();

    [HttpGet]
    [AllowAnonymous]
    public IActionResult ForgotPassword() => View();

    [HttpPost]
    [AllowAnonymous]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);

        // Always show the same confirmation to prevent email enumeration (Confidentiality)
        if (user is not null)
        {
            var token = await _userManager.GeneratePasswordResetTokenAsync(user);
            var resetLink = Url.Action(nameof(ResetPassword), "Account",
                new { token, email = model.Email }, Request.Scheme)!;
            await _emailService.SendPasswordResetEmailAsync(model.Email, resetLink);
        }

        return RedirectToAction(nameof(ForgotPasswordConfirmation));
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult ForgotPasswordConfirmation() => View();

    [HttpGet]
    [AllowAnonymous]
    public IActionResult ResetPassword(string? token, string? email)
    {
        if (token is null || email is null) return BadRequest("Invalid password reset request.");
        return View(new ResetPasswordViewModel { Token = token, Email = email });
    }

    [HttpPost]
    [AllowAnonymous]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);
        if (user is null)
        {
            // Do not reveal that user does not exist (Confidentiality)
            return RedirectToAction(nameof(ResetPasswordConfirmation));
        }

        var result = await _userManager.ResetPasswordAsync(user, model.Token, model.Password);

        if (!result.Succeeded)
        {
            AddIdentityErrors(result);
            return View(model);
        }

        await _auditService.LogAsync("PasswordReset", user.Id, null, GetClientIp());
        return RedirectToAction(nameof(ResetPasswordConfirmation));
    }

    [HttpGet]
    [AllowAnonymous]
    public IActionResult ResetPasswordConfirmation() => View();

    [HttpGet]
    [Authorize]
    public async Task<IActionResult> Profile()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is null) return NotFound();

        var model = new ProfileViewModel { UserName = user.UserName!, Email = user.Email! };
        return View(model);
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Profile(ProfileViewModel model)
    {
        // Trust boundary: validate all fields before touching identity (Integrity)
        _logger.LogInformation("Profile update by {UserId}", _userManager.GetUserId(User));

        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.GetUserAsync(User);
        if (user is null) return NotFound();

        var passwordCheck = await _userManager.CheckPasswordAsync(user, model.CurrentPassword);
        if (!passwordCheck)
        {
            ModelState.AddModelError(nameof(model.CurrentPassword), "Current password is incorrect.");
            return View(model);
        }

        return await ApplyProfileChanges(user, model);
    }

    private async Task<IActionResult> ApplyProfileChanges(ApplicationUser user, ProfileViewModel model)
    {
        user.UserName = model.UserName;
        user.Email = model.Email;
        var updateResult = await _userManager.UpdateAsync(user);

        if (!updateResult.Succeeded)
        {
            AddIdentityErrors(updateResult);
            return View(model);
        }

        if (!string.IsNullOrWhiteSpace(model.NewPassword))
        {
            var pwResult = await _userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);
            if (!pwResult.Succeeded) { AddIdentityErrors(pwResult); return View(model); }
        }

        await _auditService.LogAsync("ProfileUpdated", user.Id, $"Username={user.UserName}", GetClientIp());
        TempData["SuccessMessage"] = "Profile updated successfully.";
        return RedirectToAction(nameof(Profile));
    }

    private void AddIdentityErrors(IdentityResult result)
    {
        foreach (var error in result.Errors)
        {
            ModelState.AddModelError(string.Empty, error.Description);
        }
    }

    private IActionResult RedirectToLocal(string? returnUrl)
    {
        if (Url.IsLocalUrl(returnUrl)) return Redirect(returnUrl);
        return RedirectToAction("Index", "Notes");
    }

    private string? GetClientIp() =>
        HttpContext.Connection.RemoteIpAddress?.ToString();
}
