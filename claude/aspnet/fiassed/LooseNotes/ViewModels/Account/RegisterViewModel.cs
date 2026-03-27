using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

/// <summary>
/// Registration view model. Validation attributes document allowed formats (ASVS V2.2.1).
/// Server-side validation is authoritative; these annotations are a UX supplement only.
/// </summary>
public sealed class RegisterViewModel
{
    [Required]
    [MinLength(1)]
    [MaxLength(50)]
    [RegularExpression(@"^[a-zA-Z0-9_\-\.]+$", ErrorMessage = "Username may only contain letters, numbers, underscores, hyphens, and dots.")]
    public string Username { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    [MaxLength(256)]
    public string Email { get; set; } = string.Empty;

    [Required]
    [DataType(DataType.Password)]
    [MinLength(8, ErrorMessage = "Password must be at least 8 characters.")]
    [MaxLength(128, ErrorMessage = "Password must not exceed 128 characters.")]
    public string Password { get; set; } = string.Empty;

    [Required]
    [DataType(DataType.Password)]
    [Compare(nameof(Password), ErrorMessage = "Passwords do not match.")]
    public string ConfirmPassword { get; set; } = string.Empty;
}
