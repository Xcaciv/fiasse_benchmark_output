using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public class ForgotPasswordViewModel
{
    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;
}
