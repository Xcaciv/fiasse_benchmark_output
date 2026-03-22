using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels;

public class EditProfileViewModel
{
    [Required]
    [StringLength(50, MinimumLength = 3)]
    public string Username { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    [DataType(DataType.Password)]
    public string? CurrentPassword { get; set; }

    [StringLength(100, MinimumLength = 6)]
    [DataType(DataType.Password)]
    public string? NewPassword { get; set; }

    [DataType(DataType.Password)]
    [Compare("NewPassword", ErrorMessage = "Passwords do not match.")]
    public string? ConfirmNewPassword { get; set; }
}
