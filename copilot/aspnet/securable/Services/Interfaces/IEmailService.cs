namespace LooseNotes.Services.Interfaces;

/// <summary>
/// Email delivery abstraction. Production implementations must not log message body
/// or links to avoid token leakage (Confidentiality).
/// </summary>
public interface IEmailService
{
    /// <summary>
    /// Sends a password reset email containing the reset link.
    /// Implementations must not surface link in application logs.
    /// </summary>
    Task SendPasswordResetEmailAsync(string email, string resetLink);
}
