// RegisterViewModel.cs — Input model for the registration form.
// Integrity: Email format, password length, and confirmation match enforced via annotations.
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

/// <summary>Carries registration data from the form.
/// Confidentiality: passwords are [DataType.Password] — never stored or logged from here.</summary>
public sealed class RegisterViewModel
{
    [Required]
    [StringLength(50, MinimumLength = 3)]
    [Display(Name = "Username")]
    public string UserName { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    [Display(Name = "Email")]
    public string Email { get; set; } = string.Empty;

    [Required]
    [StringLength(100, MinimumLength = 8)]
    [DataType(DataType.Password)]
    [Display(Name = "Password")]
    public string Password { get; set; } = string.Empty;

    [Required]
    [DataType(DataType.Password)]
    [Compare(nameof(Password), ErrorMessage = "Passwords do not match.")]
    [Display(Name = "Confirm password")]
    public string ConfirmPassword { get; set; } = string.Empty;
}
