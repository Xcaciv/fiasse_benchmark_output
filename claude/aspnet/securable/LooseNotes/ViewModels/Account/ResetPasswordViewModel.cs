// ResetPasswordViewModel.cs — Carries the reset token + new password.
// Authenticity: token is validated by Identity — never executed as code.
// Confidentiality: token is a hidden field; password is [DataType.Password].
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public sealed class ResetPasswordViewModel
{
    [Required]
    public string Token { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    [Required]
    [StringLength(100, MinimumLength = 8)]
    [DataType(DataType.Password)]
    [Display(Name = "New password")]
    public string Password { get; set; } = string.Empty;

    [Required]
    [DataType(DataType.Password)]
    [Compare(nameof(Password), ErrorMessage = "Passwords do not match.")]
    [Display(Name = "Confirm new password")]
    public string ConfirmPassword { get; set; } = string.Empty;
}
