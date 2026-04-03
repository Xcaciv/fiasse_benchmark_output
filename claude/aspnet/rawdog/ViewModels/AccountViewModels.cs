using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels;

public class RegisterViewModel
{
    [Required]
    public string Username { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    [Required]
    public string Password { get; set; } = string.Empty;

    [Compare("Password", ErrorMessage = "Passwords do not match.")]
    public string ConfirmPassword { get; set; } = string.Empty;

    public string? SecurityQuestion { get; set; }
    public string? SecurityAnswer { get; set; }
}

public class LoginViewModel
{
    [Required]
    public string Username { get; set; } = string.Empty;

    [Required]
    public string Password { get; set; } = string.Empty;
}

public class ForgotPasswordViewModel
{
    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;
}

public class ResetPasswordViewModel
{
    public string Email { get; set; } = string.Empty;
    public string? SecurityQuestion { get; set; }
    public string Answer { get; set; } = string.Empty;
}

public class SecurityQuestionSetupViewModel
{
    [Required]
    public string SecurityQuestion { get; set; } = string.Empty;

    [Required]
    public string SecurityAnswer { get; set; } = string.Empty;
}
