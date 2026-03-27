using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ProfileViewModel
{
    [Required]
    [StringLength(100, MinimumLength = 3)]
    [Display(Name = "Username")]
    public string UserName { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    [StringLength(256)]
    [Display(Name = "Email")]
    public string Email { get; set; } = string.Empty;

    [Required]
    [StringLength(100, MinimumLength = 2)]
    [Display(Name = "Display Name")]
    public string DisplayName { get; set; } = string.Empty;
}
