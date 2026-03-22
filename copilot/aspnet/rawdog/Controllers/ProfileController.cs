using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using rawdog.Data;
using rawdog.Models;
using rawdog.Services;
using rawdog.ViewModels;

namespace rawdog.Controllers;

[Authorize]
public sealed class ProfileController(
    UserManager<ApplicationUser> userManager,
    SignInManager<ApplicationUser> signInManager,
    ApplicationDbContext dbContext,
    IActivityLogger activityLogger) : Controller
{
    public async Task<IActionResult> Index(CancellationToken cancellationToken)
    {
        var user = await GetCurrentUserAsync();
        var noteCount = await dbContext.Notes.CountAsync(note => note.OwnerId == user.Id, cancellationToken);

        return View(new ProfileIndexViewModel
        {
            UserName = user.UserName ?? string.Empty,
            Email = user.Email ?? string.Empty,
            RegisteredAtUtc = user.RegisteredAtUtc,
            NoteCount = noteCount
        });
    }

    public async Task<IActionResult> Edit()
    {
        var user = await GetCurrentUserAsync();
        return View(new ProfileEditViewModel
        {
            UserName = user.UserName ?? string.Empty,
            Email = user.Email ?? string.Empty
        });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(ProfileEditViewModel model, CancellationToken cancellationToken)
    {
        var user = await GetCurrentUserAsync();
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        if (!string.Equals(user.UserName, model.UserName.Trim(), StringComparison.Ordinal))
        {
            var updateUserNameResult = await userManager.SetUserNameAsync(user, model.UserName.Trim());
            if (!updateUserNameResult.Succeeded)
            {
                AddErrors(updateUserNameResult);
                return View(model);
            }
        }

        if (!string.Equals(user.Email, model.Email.Trim(), StringComparison.OrdinalIgnoreCase))
        {
            var updateEmailResult = await userManager.SetEmailAsync(user, model.Email.Trim());
            if (!updateEmailResult.Succeeded)
            {
                AddErrors(updateEmailResult);
                return View(model);
            }
        }

        await signInManager.RefreshSignInAsync(user);
        await activityLogger.LogAsync("profile.update", $"Updated profile for '{user.UserName}'.", user.Id, cancellationToken);

        TempData["StatusMessage"] = "Profile updated successfully.";
        return RedirectToAction(nameof(Index));
    }

    public IActionResult ChangePassword()
    {
        return View(new ChangePasswordViewModel());
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ChangePassword(ChangePasswordViewModel model, CancellationToken cancellationToken)
    {
        if (!ModelState.IsValid)
        {
            return View(model);
        }

        var user = await GetCurrentUserAsync();
        var result = await userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);
        if (!result.Succeeded)
        {
            AddErrors(result);
            return View(model);
        }

        await signInManager.RefreshSignInAsync(user);
        await activityLogger.LogAsync("profile.password_change", $"Changed password for '{user.UserName}'.", user.Id, cancellationToken);

        TempData["StatusMessage"] = "Password changed successfully.";
        return RedirectToAction(nameof(Index));
    }

    private async Task<ApplicationUser> GetCurrentUserAsync()
    {
        var userId = User.FindFirstValue(ClaimTypes.NameIdentifier)
            ?? throw new InvalidOperationException("The current user identifier is not available.");

        return await userManager.FindByIdAsync(userId)
            ?? throw new InvalidOperationException("The current user could not be found.");
    }

    private void AddErrors(IdentityResult result)
    {
        foreach (var error in result.Errors)
        {
            ModelState.AddModelError(string.Empty, error.Description);
        }
    }
}
