using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public sealed class ForgotPasswordViewModel
{
    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;
}
