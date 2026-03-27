// EditProfileViewModel.cs — Input model for editing user profile.
// Confidentiality: current password required to change settings (Authenticity check).
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Profile;

public sealed class EditProfileViewModel
{
    [Required]
    [StringLength(50, MinimumLength = 3)]
    [Display(Name = "Username")]
    public string UserName { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    [Display(Name = "Email")]
    public string Email { get; set; } = string.Empty;

    // ── Optional password change ───────────────────────────────────────────────
    [DataType(DataType.Password)]
    [Display(Name = "Current password (required to save changes)")]
    public string? CurrentPassword { get; set; }

    [StringLength(100, MinimumLength = 8)]
    [DataType(DataType.Password)]
    [Display(Name = "New password (leave blank to keep current)")]
    public string? NewPassword { get; set; }

    [DataType(DataType.Password)]
    [Compare(nameof(NewPassword), ErrorMessage = "Passwords do not match.")]
    [Display(Name = "Confirm new password")]
    public string? ConfirmNewPassword { get; set; }
}
