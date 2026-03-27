using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Profile;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

/// <summary>
/// User profile management: username, email, password updates.
/// Password change requires current password to prevent CSRF-based credential takeover (ASVS V6.2.2).
/// All profile changes are logged without logging the new values (Confidentiality, ASVS V16.2.1).
/// </summary>
[Authorize]
[AutoValidateAntiforgeryToken]
public sealed class ProfileController : Controller
{
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly SignInManager<ApplicationUser> _signInManager;
    private readonly IEmailService _emailService;
    private readonly IAuditService _auditService;
    private readonly IPasswordValidationService _passwordValidation;
    private readonly ILogger<ProfileController> _logger;

    public ProfileController(
        UserManager<ApplicationUser> userManager,
        SignInManager<ApplicationUser> signInManager,
        IEmailService emailService,
        IAuditService auditService,
        IPasswordValidationService passwordValidation,
        ILogger<ProfileController> logger)
    {
        _userManager = userManager;
        _signInManager = signInManager;
        _emailService = emailService;
        _auditService = auditService;
        _passwordValidation = passwordValidation;
        _logger = logger;
    }

    [HttpGet]
    public async Task<IActionResult> Edit()
    {
        var user = await _userManager.GetUserAsync(User);
        if (user == null) return Challenge();

        return View(new EditProfileViewModel
        {
            CurrentUsername = user.UserName ?? string.Empty,
            CurrentEmail = user.Email ?? string.Empty
        });
    }

    [HttpPost]
    public async Task<IActionResult> UpdateUsername(EditProfileViewModel model)
    {
        var user = await _userManager.GetUserAsync(User);
        if (user == null) return Challenge();

        if (string.IsNullOrWhiteSpace(model.NewUsername))
        {
            TempData["Error"] = "Username cannot be empty.";
            return RedirectToAction(nameof(Edit));
        }

        if (model.NewUsername == user.UserName)
        {
            TempData["Info"] = "Username unchanged.";
            return RedirectToAction(nameof(Edit));
        }

        var result = await _userManager.SetUserNameAsync(user, model.NewUsername);

        if (result.Succeeded)
        {
            await _signInManager.RefreshSignInAsync(user);

            await _auditService.LogAsync(
                AuditEventTypes.UserUsernameChanged,
                user.Id, user.UserName, GetClientIp(),
                resourceType: "user", resourceId: user.Id
                // New username not logged to avoid accidental PII in audit trail
            );

            TempData["Success"] = "Username updated successfully.";
        }
        else
        {
            // Uniqueness conflict - user-facing error
            TempData["Error"] = "Username is already taken or invalid.";
        }

        return RedirectToAction(nameof(Edit));
    }

    [HttpPost]
    public async Task<IActionResult> UpdateEmail(EditProfileViewModel model)
    {
        var user = await _userManager.GetUserAsync(User);
        if (user == null) return Challenge();

        if (string.IsNullOrWhiteSpace(model.NewEmail))
        {
            TempData["Error"] = "Email cannot be empty.";
            return RedirectToAction(nameof(Edit));
        }

        // Server-side email format validation (ASVS V2.2.1)
        if (!IsValidEmailFormat(model.NewEmail))
        {
            TempData["Error"] = "Invalid email format.";
            return RedirectToAction(nameof(Edit));
        }

        if (model.NewEmail == user.Email)
        {
            TempData["Info"] = "Email unchanged.";
            return RedirectToAction(nameof(Edit));
        }

        var token = await _userManager.GenerateChangeEmailTokenAsync(user, model.NewEmail);
        var result = await _userManager.ChangeEmailAsync(user, model.NewEmail, token);

        if (result.Succeeded)
        {
            await _signInManager.RefreshSignInAsync(user);

            await _emailService.SendEmailChangedNotificationAsync(user.Email ?? model.NewEmail, user.UserName ?? string.Empty);

            await _auditService.LogAsync(
                AuditEventTypes.UserEmailChanged,
                user.Id, user.UserName, GetClientIp(),
                resourceType: "user", resourceId: user.Id
                // New email value NOT logged (Confidentiality, FIASSE S2.6)
            );

            TempData["Success"] = "Email updated successfully.";
        }
        else
        {
            TempData["Error"] = "Email could not be updated. It may already be in use.";
        }

        return RedirectToAction(nameof(Edit));
    }

    [HttpPost]
    public async Task<IActionResult> UpdatePassword(EditProfileViewModel model)
    {
        var user = await _userManager.GetUserAsync(User);
        if (user == null) return Challenge();

        if (string.IsNullOrEmpty(model.CurrentPassword))
        {
            TempData["Error"] = "Current password is required to change your password.";
            return RedirectToAction(nameof(Edit));
        }

        if (string.IsNullOrEmpty(model.NewPassword))
        {
            TempData["Error"] = "New password is required.";
            return RedirectToAction(nameof(Edit));
        }

        // New password policy validation - same service as registration (FIASSE S2.1)
        var policyError = _passwordValidation.Validate(model.NewPassword);
        if (policyError != null)
        {
            TempData["Error"] = policyError;
            return RedirectToAction(nameof(Edit));
        }

        if (model.NewPassword != model.ConfirmNewPassword)
        {
            TempData["Error"] = "New passwords do not match.";
            return RedirectToAction(nameof(Edit));
        }

        // Requires current password - prevents CSRF-based credential takeover (ASVS V6.2.2)
        var result = await _userManager.ChangePasswordAsync(user, model.CurrentPassword, model.NewPassword);

        if (result.Succeeded)
        {
            // Invalidate all other sessions after password change (ASVS V7.4.3)
            await _userManager.UpdateSecurityStampAsync(user);
            await _signInManager.RefreshSignInAsync(user);

            await _auditService.LogAsync(
                AuditEventTypes.UserPasswordChanged,
                user.Id, user.UserName, GetClientIp(),
                resourceType: "user", resourceId: user.Id
                // Password values never logged (Confidentiality)
            );

            TempData["Success"] = "Password changed. Your other sessions have been signed out.";
        }
        else
        {
            TempData["Error"] = "Password change failed. Please verify your current password is correct.";
        }

        return RedirectToAction(nameof(Edit));
    }

    private static bool IsValidEmailFormat(string email)
    {
        // Basic RFC 5321 structure check: local@domain.tld
        try
        {
            var addr = new System.Net.Mail.MailAddress(email);
            return addr.Address == email;
        }
        catch
        {
            return false;
        }
    }

    private string GetClientIp()
        => HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";
}
