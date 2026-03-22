using LooseNotes.Models;
using LooseNotes.ViewModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

[Authorize]
public class ProfileController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly ILogger<ProfileController> _logger;

    public ProfileController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        ILogger<ProfileController> logger)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _logger = logger;
    }

    [HttpGet]
    public async Task<IActionResult> Edit()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user == null) return NotFound();

        return View(new EditProfileViewModel
        {
            UserName = user.UserName!,
            Email = user.Email!
        });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(EditProfileViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var user = await _userManager.GetUserAsync(User);
        if (user == null) return NotFound();

        // Change password if requested
        if (!string.IsNullOrEmpty(model.NewPassword))
        {
            if (string.IsNullOrEmpty(model.CurrentPassword))
            {
                ModelState.AddModelError("CurrentPassword", "Current password is required to set a new password.");
                return View(model);
            }

            var pwResult = await _userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);
            if (!pwResult.Succeeded)
            {
                foreach (var err in pwResult.Errors)
                    ModelState.AddModelError(string.Empty, err.Description);
                return View(model);
            }
        }

        // Update username/email
        if (user.UserName != model.UserName)
        {
            var setNameResult = await _userManager.SetUserNameAsync(user, model.UserName);
            if (!setNameResult.Succeeded)
            {
                foreach (var err in setNameResult.Errors)
                    ModelState.AddModelError(string.Empty, err.Description);
                return View(model);
            }
        }

        if (user.Email != model.Email)
        {
            var setEmailResult = await _userManager.SetEmailAsync(user, model.Email);
            if (!setEmailResult.Succeeded)
            {
                foreach (var err in setEmailResult.Errors)
                    ModelState.AddModelError(string.Empty, err.Description);
                return View(model);
            }
        }

        await _signInManager.RefreshSignInAsync(user);
        _logger.LogInformation("User {UserId} updated their profile.", user.Id);

        TempData["Success"] = "Profile updated successfully.";
        return RedirectToAction(nameof(Edit));
    }
}
