using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Profile;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

/// <summary>
/// User profile: view and edit display name, email, and password.
/// Requires re-verification of current password before changing credentials (Authenticity).
/// </summary>
[Authorize]
public class ProfileController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IAuditService _auditService;

    public ProfileController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IAuditService auditService)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _auditService = auditService;
    }

    [HttpGet]
    public async Task<IActionResult> Edit()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is null) return NotFound();

        var vm = new EditProfileViewModel
        {
            DisplayName = user.DisplayName,
            Email = user.Email ?? string.Empty
        };
        return View(vm);
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(EditProfileViewModel model)
    {
        if (!ModelState.IsValid)
            return View(model);

        var user = await _userManager.GetUserAsync(User);
        if (user is null) return NotFound();

        var changed = false;

        // Update display name (no credential re-verification required)
        if (user.DisplayName != model.DisplayName)
        {
            user.DisplayName = model.DisplayName;
            changed = true;
        }

        // Email change — requires current password verification (Authenticity)
        if (user.Email != model.Email)
        {
            if (!await VerifyCurrentPasswordAsync(user, model.CurrentPassword))
            {
                ModelState.AddModelError(nameof(model.CurrentPassword), "Current password is incorrect.");
                return View(model);
            }
            user.UserName = model.Email;
            user.Email = model.Email;
            user.NormalizedEmail = model.Email.ToUpperInvariant();
            user.NormalizedUserName = model.Email.ToUpperInvariant();
            changed = true;
        }

        if (changed)
        {
            var updateResult = await _userManager.UpdateAsync(user);
            if (!updateResult.Succeeded)
            {
                foreach (var error in updateResult.Errors)
                    ModelState.AddModelError(string.Empty, error.Description);
                return View(model);
            }
        }

        // Password change
        if (!string.IsNullOrEmpty(model.NewPassword))
        {
            if (!await VerifyCurrentPasswordAsync(user, model.CurrentPassword))
            {
                ModelState.AddModelError(nameof(model.CurrentPassword), "Current password is incorrect.");
                return View(model);
            }

            var pwResult = await _userManager.ChangePasswordAsync(user, model.CurrentPassword!, model.NewPassword);
            if (!pwResult.Succeeded)
            {
                foreach (var error in pwResult.Errors)
                    ModelState.AddModelError(string.Empty, error.Description);
                return View(model);
            }

            // Re-sign in after password change to refresh security stamp (Authenticity)
            await _signInManager.RefreshSignInAsync(user);
            await _auditService.RecordAsync("PasswordChanged", userId: user.Id);
        }

        await _auditService.RecordAsync("ProfileUpdated", userId: user.Id);
        TempData["Success"] = "Profile updated successfully.";
        return RedirectToAction(nameof(Edit));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private async Task<bool> VerifyCurrentPasswordAsync(ApplicationUser user, string? password)
    {
        if (string.IsNullOrWhiteSpace(password))
            return false;
        return await _userManager.CheckPasswordAsync(user, password);
    }
}
