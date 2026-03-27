using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ForgotPasswordViewModel
{
    [Required]
    [EmailAddress]
    [StringLength(256)]
    [Display(Name = "Email")]
    public string Email { get; set; } = string.Empty;
}
