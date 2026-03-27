using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Models;
using LooseNotes.ViewModels;

namespace LooseNotes.Controllers;

[Authorize]
public class ProfileController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;

    public ProfileController(UserManager<ApplicationUser> userManager)
    {
        _userManager = userManager;
    }

    [HttpGet]
    public async Task<IActionResult> Edit()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user == null) return NotFound();

        return View(new EditProfileViewModel
        {
            Username = user.UserName ?? string.Empty,
            Email = user.Email ?? string.Empty
        });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(EditProfileViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.GetUserAsync(User);
        if (user == null) return NotFound();

        user.UserName = model.Username;
        user.Email = model.Email;

        var updateResult = await _userManager.UpdateAsync(user);
        if (!updateResult.Succeeded)
        {
            foreach (var e in updateResult.Errors)
                ModelState.AddModelError(string.Empty, e.Description);
            return View(model);
        }

        if (!string.IsNullOrEmpty(model.NewPassword) && !string.IsNullOrEmpty(model.CurrentPassword))
        {
            var pwResult = await _userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);
            if (!pwResult.Succeeded)
            {
                foreach (var e in pwResult.Errors)
                    ModelState.AddModelError(string.Empty, e.Description);
                return View(model);
            }
        }

        TempData["Success"] = "Profile updated successfully.";
        return RedirectToAction(nameof(Edit));
    }
}
