using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ResetPasswordViewModel
{
    [Required, EmailAddress, MaxLength(256)]
    public string Email { get; set; } = string.Empty;

    /// <summary>
    /// Server-issued token from password reset email.
    /// Validated by Identity before accepting the new password (Authenticity).
    /// </summary>
    [Required]
    public string Token { get; set; } = string.Empty;

    [Required, DataType(DataType.Password), MinLength(8), MaxLength(128)]
    [Display(Name = "New Password")]
    public string Password { get; set; } = string.Empty;

    [Required, DataType(DataType.Password), Compare(nameof(Password))]
    [Display(Name = "Confirm Password")]
    public string ConfirmPassword { get; set; } = string.Empty;
}
