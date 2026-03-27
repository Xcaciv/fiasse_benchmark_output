using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Account;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

[Authorize]
public class ProfileController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IAuditService _audit;

    public ProfileController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IAuditService audit)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _audit = audit;
    }

    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is null) return NotFound();

        return View(new ProfileViewModel
        {
            Username = user.UserName ?? string.Empty,
            Email = user.Email ?? string.Empty
        });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Index([Bind("Username,Email,CurrentPassword,NewPassword,ConfirmNewPassword")] ProfileViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.GetUserAsync(User);
        if (user is null) return NotFound();

        if (!string.IsNullOrWhiteSpace(model.NewPassword))
        {
            if (string.IsNullOrWhiteSpace(model.CurrentPassword))
            {
                ModelState.AddModelError("CurrentPassword", "Current password is required to change password.");
                return View(model);
            }

            var changeResult = await _userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);
            if (!changeResult.Succeeded)
            {
                foreach (var error in changeResult.Errors)
                {
                    ModelState.AddModelError(string.Empty, error.Description);
                }
                return View(model);
            }

            _audit.LogAuthEvent("PASSWORD_CHANGED", user.Id, "User changed password");
            await _signInManager.RefreshSignInAsync(user);
        }

        TempData["Success"] = "Profile updated.";
        return RedirectToAction(nameof(Index));
    }
}
