namespace LooseNotes.Services;

public interface IEmailService
{
    /// <summary>
    /// Sends a password-reset email containing the provided reset link.
    /// SSEM: Only the link (not the raw token) is passed to this method.
    /// </summary>
    Task SendPasswordResetAsync(string toEmail, string resetLink);
}
