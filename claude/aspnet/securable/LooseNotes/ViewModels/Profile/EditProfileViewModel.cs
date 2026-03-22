using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Profile;

public class EditProfileViewModel
{
    [Required, MaxLength(100), Display(Name = "Display Name")]
    public string DisplayName { get; set; } = string.Empty;

    [Required, EmailAddress, MaxLength(256)]
    public string Email { get; set; } = string.Empty;

    // Password change is optional — leave blank to keep current password
    [DataType(DataType.Password), MaxLength(128)]
    [Display(Name = "Current Password")]
    public string? CurrentPassword { get; set; }

    [DataType(DataType.Password), MinLength(8), MaxLength(128)]
    [Display(Name = "New Password")]
    public string? NewPassword { get; set; }

    [DataType(DataType.Password), Compare(nameof(NewPassword))]
    [Display(Name = "Confirm New Password")]
    public string? ConfirmNewPassword { get; set; }
}
