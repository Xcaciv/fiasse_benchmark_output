using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ForgotPasswordViewModel
{
    [Required, EmailAddress, MaxLength(256)]
    [Display(Name = "Email Address")]
    public string Email { get; set; } = string.Empty;
}
