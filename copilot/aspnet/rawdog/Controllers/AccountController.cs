using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.ViewModels.Account;

namespace LooseNotes.Controllers;

public class AccountController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly ApplicationDbContext _context;

    public AccountController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        ApplicationDbContext context)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _context = context;
    }

    private async Task LogActivityAsync(string action, string entityType, string entityId, string? userId)
    {
        _context.ActivityLogs.Add(new ActivityLog
        {
            Action = action,
            EntityType = entityType,
            EntityId = entityId,
            Timestamp = DateTime.UtcNow,
            UserId = userId
        });
        await _context.SaveChangesAsync();
    }

    [HttpGet]
    public IActionResult Register() => View();

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Register(RegisterViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = new ApplicationUser
        {
            UserName = model.UserName,
            Email = model.Email,
            CreatedAt = DateTime.UtcNow
        };

        var result = await _userManager.CreateAsync(user, model.Password);
        if (result.Succeeded)
        {
            await _userManager.AddToRoleAsync(user, "User");
            await _signInManager.SignInAsync(user, isPersistent: false);
            await LogActivityAsync("Register", "User", user.Id, user.Id);
            return RedirectToAction("Index", "Notes");
        }

        foreach (var error in result.Errors)
            ModelState.AddModelError(string.Empty, error.Description);

        return View(model);
    }

    [HttpGet]
    public IActionResult Login(string? returnUrl = null)
    {
        ViewBag.ReturnUrl = returnUrl;
        return View();
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Login(LoginViewModel model, string? returnUrl = null)
    {
        if (!ModelState.IsValid) return View(model);

        var result = await _signInManager.PasswordSignInAsync(model.UserName, model.Password, model.RememberMe, false);
        if (result.Succeeded)
        {
            var user = await _userManager.FindByNameAsync(model.UserName);
            if (user != null)
                await LogActivityAsync("Login", "User", user.Id, user.Id);

            if (!string.IsNullOrEmpty(returnUrl) && Url.IsLocalUrl(returnUrl))
                return Redirect(returnUrl);
            return RedirectToAction("Index", "Notes");
        }

        ModelState.AddModelError(string.Empty, "Invalid login attempt.");
        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Logout()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user != null)
            await LogActivityAsync("Logout", "User", user.Id, user.Id);
        await _signInManager.SignOutAsync();
        return RedirectToAction("Index", "Home");
    }

    [HttpGet]
    public IActionResult ForgotPassword() => View();

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ForgotPassword(ForgotPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);
        if (user == null)
        {
            TempData["Info"] = "If that email is registered, a reset link has been generated.";
            return RedirectToAction("ForgotPasswordConfirmation");
        }

        var token = await _userManager.GeneratePasswordResetTokenAsync(user);
        var resetLink = Url.Action("ResetPassword", "Account",
            new { token = token, email = model.Email }, Request.Scheme);
        TempData["ResetLink"] = resetLink;
        return RedirectToAction("ForgotPasswordConfirmation");
    }

    [HttpGet]
    public IActionResult ForgotPasswordConfirmation() => View();

    [HttpGet]
    public IActionResult ResetPassword(string token, string email)
    {
        var model = new ResetPasswordViewModel { Token = token, Email = email };
        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ResetPassword(ResetPasswordViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.FindByEmailAsync(model.Email);
        if (user == null)
        {
            ModelState.AddModelError(string.Empty, "Invalid request.");
            return View(model);
        }

        var result = await _userManager.ResetPasswordAsync(user, model.Token, model.Password);
        if (result.Succeeded)
        {
            TempData["Success"] = "Password reset successfully. Please log in.";
            return RedirectToAction("Login");
        }

        foreach (var error in result.Errors)
            ModelState.AddModelError(string.Empty, error.Description);

        return View(model);
    }

    [HttpGet]
    [Microsoft.AspNetCore.Authorization.Authorize]
    public async Task<IActionResult> Profile()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user == null) return NotFound();

        var model = new ProfileViewModel
        {
            UserName = user.UserName ?? string.Empty,
            Email = user.Email ?? string.Empty
        };
        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    [Microsoft.AspNetCore.Authorization.Authorize]
    public async Task<IActionResult> Profile(ProfileViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.GetUserAsync(User);
        if (user == null) return NotFound();

        user.UserName = model.UserName;
        user.Email = model.Email;

        if (!string.IsNullOrEmpty(model.NewPassword))
        {
            if (string.IsNullOrEmpty(model.CurrentPassword))
            {
                ModelState.AddModelError(string.Empty, "Current password is required to set a new password.");
                return View(model);
            }

            var passwordResult = await _userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);
            if (!passwordResult.Succeeded)
            {
                foreach (var error in passwordResult.Errors)
                    ModelState.AddModelError(string.Empty, error.Description);
                return View(model);
            }
        }

        var updateResult = await _userManager.UpdateAsync(user);
        if (updateResult.Succeeded)
        {
            TempData["Success"] = "Profile updated successfully.";
            return RedirectToAction("Profile");
        }

        foreach (var error in updateResult.Errors)
            ModelState.AddModelError(string.Empty, error.Description);

        return View(model);
    }

    public IActionResult AccessDenied() => View();
}
