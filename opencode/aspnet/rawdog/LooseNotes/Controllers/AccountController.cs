using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;

namespace LooseNotes.Controllers;

public class AccountController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IActivityLogService _activityLog;

    public AccountController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IActivityLogService activityLog)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _activityLog = activityLog;
    }

    [HttpGet]
    public IActionResult Register()
    {
        return View();
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        if (ModelState.IsValid)
        {
            var user = new ApplicationUser
            {
                UserName = model.UserName,
                Email = model.Email,
                DisplayName = model.UserName,
                CreatedAt = DateTime.UtcNow
            };

            var result = await _userManager.CreateAsync(user, model.Password);

            if (result.Succeeded)
            {
                await _userManager.AddToRoleAsync(user, "User");
                await _activityLog.LogAsync("User Registered", user.Id, $"User {user.UserName} registered");
                await _signInManager.SignInAsync(user, isPersistent: false);
                return RedirectToAction("Index", "Home");
            }

            foreach (var error in result.Errors)
            {
                ModelState.AddModelError(string.Empty, error.Description);
            }
        }

        return View(model);
    }

    [HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;
        return View();
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Login(LoginViewModel model, string? returnUrl = null)
    {
        ViewData["ReturnUrl"] = returnUrl;

        if (ModelState.IsValid)
        {
            var result = await _signInManager.PasswordSignInAsync(
                model.UserName,
                model.Password,
                model.RememberMe,
                lockoutOnFailure: false);

            if (result.Succeeded)
            {
                var user = await _userManager.FindByNameAsync(model.UserName);
                if (user != null)
                {
                    user.LastLoginAt = DateTime.UtcNow;
                    await _userManager.UpdateAsync(user);
                }

                await _activityLog.LogAsync("User Login", user?.Id, "User logged in successfully");
                
                if (!string.IsNullOrEmpty(returnUrl) && Url.IsLocalUrl(returnUrl))
                    return Redirect(returnUrl);
                
                return RedirectToAction("Index", "Home");
            }

            await _activityLog.LogAsync("Failed Login Attempt", null, $"Failed login for {model.UserName}");
            ModelState.AddModelError(string.Empty, "Invalid username or password.");
        }

        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Logout()
    {
        var userId = _userManager.GetUserId(User);
        await _activityLog.LogAsync("User Logout", userId, "User logged out");
        await _signInManager.SignOutAsync();
        return RedirectToAction("Index", "Home");
    }

    [HttpGet]
    [Authorize]
    public async Task<IActionResult> Profile()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user == null)
            return NotFound();

        var model = new ProfileViewModel
        {
            UserName = user.UserName ?? string.Empty,
            Email = user.Email ?? string.Empty,
            DisplayName = user.DisplayName
        };

        return View(model);
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Profile(ProfileViewModel model)
    {
        if (ModelState.IsValid)
        {
            var user = await _userManager.GetUserAsync(User);
            if (user == null)
                return NotFound();

            user.UserName = model.UserName;
            user.Email = model.Email;
            user.DisplayName = model.DisplayName;

            var result = await _userManager.UpdateAsync(user);

            if (result.Succeeded)
            {
                await _activityLog.LogAsync("Profile Updated", user.Id, "User updated their profile");
                TempData["SuccessMessage"] = "Profile updated successfully.";
                return RedirectToAction("Profile");
            }

            foreach (var error in result.Errors)
            {
                ModelState.AddModelError(string.Empty, error.Description);
            }
        }

        return View(model);
    }

    [HttpGet]
    [Authorize]
    public IActionResult ChangePassword()
    {
        return View();
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ChangePassword(ChangePasswordViewModel model)
    {
        if (ModelState.IsValid)
        {
            var user = await _userManager.GetUserAsync(User);
            if (user == null)
                return NotFound();

            var result = await _userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);

            if (result.Succeeded)
            {
                await _activityLog.LogAsync("Password Changed", user.Id, "User changed their password");
                TempData["SuccessMessage"] = "Password changed successfully.";
                return RedirectToAction("Profile");
            }

            foreach (var error in result.Errors)
            {
                ModelState.AddModelError(string.Empty, error.Description);
            }
        }

        return View(model);
    }

    [HttpGet]
    public IActionResult ForgotPassword()
    {
        return View();
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model)
    {
        if (ModelState.IsValid)
        {
            var user = await _userManager.FindByEmailAsync(model.Email);
            if (user == null)
            {
                TempData["SuccessMessage"] = "If an account exists with that email, a password reset link has been sent.";
                return RedirectToAction("Login");
            }

            var token = await _userManager.GeneratePasswordResetTokenAsync(user);
            await _activityLog.LogAsync("Password Reset Requested", user.Id, "Password reset was requested");

            TempData["SuccessMessage"] = "If an account exists with that email, a password reset link has been sent.";
            return RedirectToAction("Login");
        }

        return View(model);
    }

    [HttpGet]
    public IActionResult ResetPassword(string? token = null, string? email = null)
    {
        if (token == null || email == null)
        {
            return BadRequest("A token and email must be supplied for password reset.");
        }

        var model = new ResetPasswordViewModel { Token = token, Email = email };
        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model)
    {
        if (ModelState.IsValid)
        {
            var user = await _userManager.FindByEmailAsync(model.Email);
            if (user == null)
            {
                TempData["SuccessMessage"] = "Password has been reset successfully.";
                return RedirectToAction("Login");
            }

            var result = await _userManager.ResetPasswordAsync(user, model.Token, model.Password);

            if (result.Succeeded)
            {
                await _activityLog.LogAsync("Password Reset Completed", user.Id, "Password was reset successfully");
                TempData["SuccessMessage"] = "Password has been reset successfully.";
                return RedirectToAction("Login");
            }

            foreach (var error in result.Errors)
            {
                ModelState.AddModelError(string.Empty, error.Description);
            }
        }

        return View(model);
    }
}
