using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ProfileViewModel
{
    [Required, MaxLength(100)]
    [Display(Name = "Username")]
    public string UserName { get; set; } = string.Empty;

    [Required, EmailAddress, MaxLength(256)]
    [Display(Name = "Email Address")]
    public string Email { get; set; } = string.Empty;

    [DataType(DataType.Password)]
    [Display(Name = "Current Password")]
    public string? CurrentPassword { get; set; }

    [StringLength(100, MinimumLength = 10)]
    [DataType(DataType.Password)]
    [Display(Name = "New Password")]
    public string? NewPassword { get; set; }

    [DataType(DataType.Password)]
    [Display(Name = "Confirm New Password")]
    [Compare(nameof(NewPassword), ErrorMessage = "Passwords do not match.")]
    public string? ConfirmNewPassword { get; set; }
}
