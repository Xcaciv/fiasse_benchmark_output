using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ResetPasswordViewModel
{
    [Required]
    [EmailAddress]
    [StringLength(256)]
    public string Email { get; set; } = string.Empty;

    [Required]
    public string Token { get; set; } = string.Empty;

    [Required]
    [StringLength(100, MinimumLength = 8)]
    [DataType(DataType.Password)]
    [Display(Name = "New Password")]
    public string NewPassword { get; set; } = string.Empty;

    [Required]
    [DataType(DataType.Password)]
    [Compare(nameof(NewPassword), ErrorMessage = "Passwords do not match.")]
    [Display(Name = "Confirm New Password")]
    public string ConfirmNewPassword { get; set; } = string.Empty;
}
