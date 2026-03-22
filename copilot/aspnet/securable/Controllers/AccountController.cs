using System.Net;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Account;

namespace LooseNotes.Controllers;

[AllowAnonymous]
public sealed class AccountController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IActivityLogService _activityLogService;
    private readonly IEmailDispatchService _emailDispatchService;

    public AccountController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IActivityLogService activityLogService,
        IEmailDispatchService emailDispatchService)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _activityLogService = activityLogService;
        _emailDispatchService = emailDispatchService;
    }

    [HttpGet]
    public IActionResult Register()
    {
        if (User.Identity?.IsAuthenticated == true)
        {
            return RedirectToAction("Index", "Notes");
        }

        return View(new RegisterViewModel());
    }

    [HttpPost]
    public async Task<IActionResult> Register(RegisterViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var user = new ApplicationUser
        {
            UserName = model.UserName.Trim(),
            Email = model.Email.Trim(),
            RegisteredAtUtc = DateTime.UtcNow,
            EmailConfirmed = true
        };

        var createResult = await _userManager.CreateAsync(user, model.Password);
        if (!createResult.Succeeded)
        {
            AddIdentityErrors(createResult.Errors);
            return View(model);
        }

        await _userManager.AddToRoleAsync(user, "User");
        await _signInManager.SignInAsync(user, isPersistent: false);
        await _activityLogService.LogAsync("auth.register", $"User '{user.UserName}' registered.", user.Id, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);

        TempData["StatusMessage"] = "Welcome to Loose Notes. Your account has been created.";
        return RedirectToAction("Index", "Notes");
    }

    [HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        if (User.Identity?.IsAuthenticated == true)
        {
            return RedirectToAction("Index", "Notes");
        }

        return View(new LoginViewModel { ReturnUrl = returnUrl });
    }

    [HttpPost]
    public async Task<IActionResult> Login(LoginViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var signInResult = await _signInManager.PasswordSignInAsync(model.UserName.Trim(), model.Password, model.RememberMe, lockoutOnFailure: true);
        if (signInResult.Succeeded)
        {
            var user = await _userManager.FindByNameAsync(model.UserName.Trim());
            if (user is not null)
            {
                await _activityLogService.LogAsync("auth.login", $"User '{user.UserName}' signed in.", user.Id, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
            }

            return RedirectToLocal(model.ReturnUrl);
        }

        await _activityLogService.LogAsync("auth.login_failed", $"Failed sign-in attempt for username '{model.UserName}'.", null, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);

        if (signInResult.IsLockedOut)
        {
            ModelState.AddModelError(string.Empty, "Your account is temporarily locked because of repeated failed sign-in attempts.");
            return View(model);
        }

        ModelState.AddModelError(string.Empty, "Invalid username or password.");
        return View(model);
    }

    [Authorize]
    [HttpPost]
    public async Task<IActionResult> Logout(CancellationToken cancellationToken)
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is not null)
        {
            await _activityLogService.LogAsync("auth.logout", $"User '{user.UserName}' signed out.", user.Id, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        }

        await _signInManager.SignOutAsync();
        TempData["StatusMessage"] = "You have been signed out.";
        return RedirectToAction("Index", "Home");
    }

    [HttpGet]
    public IActionResult ForgotPassword()
    {
        return View(new ForgotPasswordViewModel());
    }

    [HttpPost]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var user = await _userManager.Users.SingleOrDefaultAsync(x => x.Email == model.Email.Trim(), cancellationToken);
        if (user is not null)
        {
            var token = await _userManager.GeneratePasswordResetTokenAsync(user);
            var resetUrl = Url.Action(nameof(ResetPassword), "Account", new { email = user.Email, token = WebUtility.UrlEncode(token) }, Request.Scheme);
            if (!string.IsNullOrWhiteSpace(resetUrl))
            {
                await _emailDispatchService.SendPasswordResetAsync(user.Email!, resetUrl, cancellationToken);
            }
        }

        TempData["StatusMessage"] = "If the email exists, a reset link has been written to the local email outbox.";
        return RedirectToAction(nameof(Login));
    }

    [HttpGet]
    public IActionResult ResetPassword(string email, string token)
    {
        return View(new ResetPasswordViewModel
        {
            Email = email,
            Token = WebUtility.UrlDecode(token)
        });
    }

    [HttpPost]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var user = await _userManager.FindByEmailAsync(model.Email.Trim());
        if (user is null)
        {
            ModelState.AddModelError(string.Empty, "Invalid reset request.");
            return View(model);
        }

        var resetResult = await _userManager.ResetPasswordAsync(user, model.Token, model.Password);
        if (!resetResult.Succeeded)
        {
            AddIdentityErrors(resetResult.Errors);
            return View(model);
        }

        await _activityLogService.LogAsync("auth.password_reset", $"User '{user.UserName}' reset their password.", user.Id, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "Your password has been reset. You can now sign in.";
        return RedirectToAction(nameof(Login));
    }

    private IActionResult RedirectToLocal(string? returnUrl)
    {
        if (!string.IsNullOrWhiteSpace(returnUrl) && Url.IsLocalUrl(returnUrl))
        {
            return Redirect(returnUrl);
        }

        return RedirectToAction("Index", "Notes");
    }

    private void AddIdentityErrors(IEnumerable<IdentityError> errors)
    {
        foreach (var error in errors)
        {
            ModelState.AddModelError(string.Empty, error.Description);
        }
    }
}
