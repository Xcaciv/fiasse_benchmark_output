using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

/// <summary>
/// Input model for user login. Trust boundary: all fields validated server-side.
/// </summary>
public class LoginViewModel
{
    [Required, EmailAddress, MaxLength(256)]
    public string Email { get; set; } = string.Empty;

    [Required, DataType(DataType.Password), MaxLength(128)]
    public string Password { get; set; } = string.Empty;

    [Display(Name = "Remember me")]
    public bool RememberMe { get; set; }

    /// <summary>Local redirect URL validated before use (Integrity).</summary>
    public string? ReturnUrl { get; set; }
}
