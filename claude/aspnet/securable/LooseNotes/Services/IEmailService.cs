namespace LooseNotes.Services;

/// <summary>
/// Email dispatch abstraction — injectable for testing without network side effects.
/// </summary>
public interface IEmailService
{
    /// <summary>
    /// Sends a password-reset email containing the reset link.
    /// Implementation must not log the token or link (Confidentiality).
    /// </summary>
    Task SendPasswordResetAsync(string toEmail, string resetLink, CancellationToken cancellationToken = default);
}
