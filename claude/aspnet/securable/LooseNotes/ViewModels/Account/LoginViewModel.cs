// LoginViewModel.cs — Input model for the login form.
// Integrity: DataAnnotations enforce required fields and format at the trust boundary.
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

/// <summary>Carries login credentials from the form to the controller.
/// Confidentiality: password is [DataType.Password] — never echoed in responses.</summary>
public sealed class LoginViewModel
{
    [Required]
    [Display(Name = "Username or Email")]
    public string UserName { get; set; } = string.Empty;

    [Required]
    [DataType(DataType.Password)]
    public string Password { get; set; } = string.Empty;

    [Display(Name = "Remember me")]
    public bool RememberMe { get; set; }
}
