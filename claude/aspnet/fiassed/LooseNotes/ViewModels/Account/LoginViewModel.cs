using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Account;

public sealed class LoginViewModel
{
    [Required]
    [MaxLength(50)]
    public string Username { get; set; } = string.Empty;

    [Required]
    [DataType(DataType.Password)]
    public string Password { get; set; } = string.Empty;

    public string? ReturnUrl { get; set; }
}
