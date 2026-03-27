using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Profile;

public sealed class EditProfileViewModel
{
    public string CurrentUsername { get; set; } = string.Empty;
    public string CurrentEmail { get; set; } = string.Empty;

    [MinLength(1)]
    [MaxLength(50)]
    [RegularExpression(@"^[a-zA-Z0-9_\-\.]+$")]
    public string? NewUsername { get; set; }

    [EmailAddress]
    [MaxLength(256)]
    public string? NewEmail { get; set; }

    // Password change section
    [DataType(DataType.Password)]
    public string? CurrentPassword { get; set; }

    [DataType(DataType.Password)]
    [MinLength(8)]
    [MaxLength(128)]
    public string? NewPassword { get; set; }

    [DataType(DataType.Password)]
    [Compare(nameof(NewPassword))]
    public string? ConfirmNewPassword { get; set; }
}
