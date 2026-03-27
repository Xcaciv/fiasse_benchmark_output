using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class LoginViewModel
{
    [Required]
    [EmailAddress]
    [StringLength(256)]
    [Display(Name = "Email")]
    public string Email { get; set; } = string.Empty;

    [Required]
    [DataType(DataType.Password)]
    [StringLength(100)]
    [Display(Name = "Password")]
    public string Password { get; set; } = string.Empty;

    [Display(Name = "Remember me")]
    public bool RememberMe { get; set; }
}
