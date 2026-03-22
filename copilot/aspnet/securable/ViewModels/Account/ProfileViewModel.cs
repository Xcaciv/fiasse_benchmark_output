using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ProfileViewModel
{
    [Required]
    [MaxLength(50)]
    [Display(Name = "Username")]
    public string UserName { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    [MaxLength(254)]
    [Display(Name = "Email")]
    public string Email { get; set; } = string.Empty;

    [DataType(DataType.Password)]
    [MinLength(8)]
    [MaxLength(100)]
    [Display(Name = "New Password (leave blank to keep current)")]
    public string? NewPassword { get; set; }

    [DataType(DataType.Password)]
    [Compare(nameof(NewPassword), ErrorMessage = "Passwords do not match.")]
    [Display(Name = "Confirm New Password")]
    public string? ConfirmNewPassword { get; set; }

    [Required]
    [DataType(DataType.Password)]
    [Display(Name = "Current Password")]
    public string CurrentPassword { get; set; } = string.Empty;
}
