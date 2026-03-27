// ProfileController.cs — User profile viewing and editing.
// Authenticity: password change requires current password verification.
// Accountability: profile changes are audited.
// Confidentiality: errors reveal minimal information.
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Profile;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

[Authorize]
public sealed class ProfileController : Controller
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

    // ── GET /Profile/Edit ─────────────────────────────────────────────────────
    [HttpGet]
    public async Task<IActionResult> Edit()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user is null) return Challenge();

        return View(new EditProfileViewModel
        {
            UserName = user.UserName!,
            Email = user.Email!
        });
    }

    // ── POST /Profile/Edit ────────────────────────────────────────────────────
    [HttpPost]
    public async Task<IActionResult> Edit(EditProfileViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.GetUserAsync(User);
        if (user is null) return Challenge();

        // Authenticity: require current password to mutate profile (prevents session hijack)
        if (string.IsNullOrEmpty(model.CurrentPassword))
        {
            ModelState.AddModelError(nameof(model.CurrentPassword),
                "Current password is required to save changes.");
            return View(model);
        }

        var passwordValid = await _userManager.CheckPasswordAsync(user, model.CurrentPassword);
        if (!passwordValid)
        {
            ModelState.AddModelError(nameof(model.CurrentPassword), "Current password is incorrect.");
            return View(model);
        }

        await UpdateUsernameAndEmailAsync(user, model);
        await UpdatePasswordIfRequestedAsync(user, model);

        if (!ModelState.IsValid) return View(model);

        // Refresh auth cookie after identity changes
        await _signInManager.RefreshSignInAsync(user);
        await _auditService.LogAsync(user.Id, "User.ProfileUpdated", "User", user.Id);

        TempData["SuccessMessage"] = "Profile updated successfully.";
        return RedirectToAction(nameof(Edit));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private async Task UpdateUsernameAndEmailAsync(ApplicationUser user, EditProfileViewModel model)
    {
        if (user.UserName != model.UserName)
        {
            var result = await _userManager.SetUserNameAsync(user, model.UserName);
            AddIdentityErrors(result);
        }

        if (user.Email != model.Email)
        {
            var result = await _userManager.SetEmailAsync(user, model.Email);
            AddIdentityErrors(result);
        }
    }

    private async Task UpdatePasswordIfRequestedAsync(ApplicationUser user, EditProfileViewModel model)
    {
        if (string.IsNullOrEmpty(model.NewPassword)) return;

        // Authenticity: use ChangePasswordAsync (validates old password again internally)
        var result = await _userManager.ChangePasswordAsync(
            user, model.CurrentPassword!, model.NewPassword);

        AddIdentityErrors(result);
    }

    private void AddIdentityErrors(IdentityResult result)
    {
        foreach (var error in result.Errors)
            ModelState.AddModelError(string.Empty, error.Description);
    }
}
