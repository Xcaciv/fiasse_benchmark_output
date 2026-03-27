using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels;

public class EditProfileViewModel
{
    [Required]
    public string Username { get; set; } = string.Empty;

    [Required, EmailAddress]
    public string Email { get; set; } = string.Empty;

    [DataType(DataType.Password)]
    public string? CurrentPassword { get; set; }

    [DataType(DataType.Password), MinLength(6)]
    public string? NewPassword { get; set; }

    [DataType(DataType.Password), Compare(nameof(NewPassword))]
    public string? ConfirmNewPassword { get; set; }
}
