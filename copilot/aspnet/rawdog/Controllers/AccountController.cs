using System.Security.Claims;
using System.Text.Encodings.Web;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Identity.UI.Services;
using Microsoft.AspNetCore.Mvc;
using rawdog.Models;
using rawdog.ViewModels;
using rawdog.Services;

namespace rawdog.Controllers;

public sealed class AccountController(
    UserManager<ApplicationUser> userManager,
    SignInManager<ApplicationUser> signInManager,
    IEmailSender emailSender,
    IActivityLogger activityLogger,
    ILogger<AccountController> logger) : Controller
{
    [AllowAnonymous]
    public IActionResult Register()
    {
        return View(new RegisterViewModel());
    }

    [HttpPost]
    [AllowAnonymous]
    [ValidateAntiForgeryToken]
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
            EmailConfirmed = true,
            RegisteredAtUtc = DateTime.UtcNow
        };

        var createResult = await userManager.CreateAsync(user, model.Password);
        if (!createResult.Succeeded)
        {
            AddErrors(createResult);
            return View(model);
        }

        var addToRoleResult = await userManager.AddToRoleAsync(user, "User");
        if (!addToRoleResult.Succeeded)
        {
            AddErrors(addToRoleResult);
            return View(model);
        }

        await signInManager.SignInAsync(user, isPersistent: false);
        await activityLogger.LogAsync("auth.register", $"User '{user.UserName}' registered.", user.Id, cancellationToken);

        TempData["StatusMessage"] = "Your account has been created.";
        return RedirectToAction("Index", "Home");
    }

    [AllowAnonymous]
    public IActionResult Login(string? returnUrl = null)
    {
        return View(new LoginViewModel { ReturnUrl = returnUrl });
    }

    [HttpPost]
    [AllowAnonymous]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Login(LoginViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var result = await signInManager.PasswordSignInAsync(model.UserName.Trim(), model.Password, model.RememberMe, lockoutOnFailure: true);
        if (result.Succeeded)
        {
            var user = await userManager.FindByNameAsync(model.UserName.Trim());
            await activityLogger.LogAsync("auth.login", $"User '{model.UserName}' logged in.", user?.Id, cancellationToken);

            if (!string.IsNullOrWhiteSpace(model.ReturnUrl) && Url.IsLocalUrl(model.ReturnUrl))
            {
                return Redirect(model.ReturnUrl);
            }

            TempData["StatusMessage"] = "Welcome back.";
            return RedirectToAction("Index", "Home");
        }

        if (result.IsLockedOut)
        {
            ModelState.AddModelError(string.Empty, "This account has been locked due to too many failed sign-in attempts. Please try again later.");
        }
        else
        {
            logger.LogWarning("Failed login attempt for username {UserName}", model.UserName);
            await activityLogger.LogAsync("auth.login_failed", $"Failed login attempt for username '{model.UserName}'.", null, cancellationToken);
            ModelState.AddModelError(string.Empty, "Invalid username or password.");
        }

        return View(model);
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Logout(CancellationToken cancellationToken)
    {
        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier);
        var userName = User.Identity?.Name ?? "Unknown";

        await signInManager.SignOutAsync();
        await activityLogger.LogAsync("auth.logout", $"User '{userName}' logged out.", userId, cancellationToken);

        TempData["StatusMessage"] = "You have been signed out.";
        return RedirectToAction("Index", "Home");
    }

    [AllowAnonymous]
    public IActionResult ForgotPassword()
    {
        return View(new ForgotPasswordViewModel());
    }

    [HttpPost]
    [AllowAnonymous]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var user = await userManager.FindByEmailAsync(model.Email.Trim());
        if (user is not null)
        {
            var token = await userManager.GeneratePasswordResetTokenAsync(user);
            var callbackUrl = Url.Action(
                nameof(ResetPassword),
                "Account",
                new { email = user.Email, token },
                Request.Scheme);

            if (callbackUrl is not null)
            {
                var encodedLink = HtmlEncoder.Default.Encode(callbackUrl);
                await emailSender.SendEmailAsync(
                    user.Email!,
                    "Loose Notes password reset",
                    $"<p>Reset your password within one hour using this link:</p><p><a href=\"{encodedLink}\">{encodedLink}</a></p>");

                await activityLogger.LogAsync("auth.password_reset_requested", $"Password reset requested for '{user.UserName}'.", user.Id, cancellationToken);
            }
        }

        TempData["StatusMessage"] = "If an account matches that email address, a password reset message has been generated.";
        return RedirectToAction(nameof(Login));
    }

    [AllowAnonymous]
    public IActionResult ResetPassword(string email, string token)
    {
        return View(new ResetPasswordViewModel
        {
            Email = email,
            Token = token
        });
    }

    [HttpPost]
    [AllowAnonymous]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var user = await userManager.FindByEmailAsync(model.Email.Trim());
        if (user is null)
        {
            TempData["ErrorMessage"] = "The password reset link is invalid or has expired.";
            return RedirectToAction(nameof(Login));
        }

        var result = await userManager.ResetPasswordAsync(user, model.Token, model.Password);
        if (!result.Succeeded)
        {
            AddErrors(result);
            return View(model);
        }

        await activityLogger.LogAsync("auth.password_reset_completed", $"Password reset completed for '{user.UserName}'.", user.Id, cancellationToken);
        TempData["StatusMessage"] = "Your password has been reset. You can now sign in.";
        return RedirectToAction(nameof(Login));
    }

    [AllowAnonymous]
    public IActionResult AccessDenied()
    {
        return View();
    }

    private void AddErrors(IdentityResult result)
    {
        foreach (var error in result.Errors)
        {
            ModelState.AddModelError(string.Empty, error.Description);
        }
    }
}
