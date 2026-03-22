using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ResetPasswordViewModel
{
    [Required]
    public string UserId { get; set; } = string.Empty;

    /// <summary>Time-limited token issued by Identity – validated server-side before use.</summary>
    [Required]
    public string Token { get; set; } = string.Empty;

    [Required]
    [StringLength(100, MinimumLength = 10)]
    [DataType(DataType.Password)]
    [Display(Name = "New Password")]
    public string NewPassword { get; set; } = string.Empty;

    [Required]
    [DataType(DataType.Password)]
    [Display(Name = "Confirm New Password")]
    [Compare(nameof(NewPassword), ErrorMessage = "Passwords do not match.")]
    public string ConfirmPassword { get; set; } = string.Empty;
}
