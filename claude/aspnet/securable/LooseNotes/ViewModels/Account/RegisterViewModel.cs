using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

/// <summary>
/// Input model for new user registration.
/// DisplayName is user-facing only; Email becomes the Identity username.
/// </summary>
public class RegisterViewModel
{
    [Required, MaxLength(100), Display(Name = "Display Name")]
    public string DisplayName { get; set; } = string.Empty;

    [Required, EmailAddress, MaxLength(256)]
    public string Email { get; set; } = string.Empty;

    [Required, DataType(DataType.Password), MinLength(8), MaxLength(128)]
    public string Password { get; set; } = string.Empty;

    [Required, DataType(DataType.Password), Compare(nameof(Password))]
    [Display(Name = "Confirm Password")]
    public string ConfirmPassword { get; set; } = string.Empty;
}
