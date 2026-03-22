using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Profile;

namespace LooseNotes.Controllers;

[Authorize]
public sealed class ProfileController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IActivityLogService _activityLogService;

    public ProfileController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IActivityLogService activityLogService)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _activityLogService = activityLogService;
    }

    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is null)
        {
            return Challenge();
        }

        ViewBag.ChangePassword = new ChangePasswordViewModel();
        return View(new ProfileViewModel
        {
            UserName = user.UserName ?? string.Empty,
            Email = user.Email ?? string.Empty,
            RegisteredAtUtc = user.RegisteredAtUtc
        });
    }

    [HttpPost]
    public async Task<IActionResult> Update(ProfileViewModel model, CancellationToken cancellationToken)
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is null)
        {
            return Challenge();
        }

        if (!ModelState.IsValid)
        {
            ViewBag.ChangePassword = new ChangePasswordViewModel();
            return View("Index", model);
        }

        user.UserName = model.UserName.Trim();
        user.Email = model.Email.Trim();

        var updateResult = await _userManager.UpdateAsync(user);
        if (!updateResult.Succeeded)
        {
            foreach (var error in updateResult.Errors)
            {
                ModelState.AddModelError(string.Empty, error.Description);
            }

            ViewBag.ChangePassword = new ChangePasswordViewModel();
            return View("Index", model);
        }

        await _signInManager.RefreshSignInAsync(user);
        await _activityLogService.LogAsync("profile.updated", $"User '{user.UserName}' updated their profile.", user.Id, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "Your profile was updated.";
        return RedirectToAction(nameof(Index));
    }

    [HttpPost]
    public async Task<IActionResult> ChangePassword(ChangePasswordViewModel model, CancellationToken cancellationToken)
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is null)
        {
            return Challenge();
        }

        var profileModel = new ProfileViewModel
        {
            UserName = user.UserName ?? string.Empty,
            Email = user.Email ?? string.Empty,
            RegisteredAtUtc = user.RegisteredAtUtc
        };

        if (!ModelState.IsValid)
        {
            ViewBag.ChangePassword = model;
            return View("Index", profileModel);
        }

        var changeResult = await _userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);
        if (!changeResult.Succeeded)
        {
            foreach (var error in changeResult.Errors)
            {
                ModelState.AddModelError(string.Empty, error.Description);
            }

            ViewBag.ChangePassword = model;
            return View("Index", profileModel);
        }

        await _signInManager.RefreshSignInAsync(user);
        await _activityLogService.LogAsync("profile.password_changed", $"User '{user.UserName}' changed their password.", user.Id, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "Your password was changed.";
        return RedirectToAction(nameof(Index));
    }
}
