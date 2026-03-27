using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ProfileViewModel
{
    [StringLength(50, MinimumLength = 3)]
    [Display(Name = "Username")]
    public string Username { get; set; } = string.Empty;

    [EmailAddress]
    [StringLength(256)]
    [Display(Name = "Email")]
    public string Email { get; set; } = string.Empty;

    [DataType(DataType.Password)]
    [Display(Name = "Current Password")]
    public string? CurrentPassword { get; set; }

    [StringLength(100, MinimumLength = 8)]
    [DataType(DataType.Password)]
    [Display(Name = "New Password")]
    public string? NewPassword { get; set; }

    [DataType(DataType.Password)]
    [Compare(nameof(NewPassword), ErrorMessage = "Passwords do not match.")]
    [Display(Name = "Confirm New Password")]
    public string? ConfirmNewPassword { get; set; }
}
