// IEmailService.cs — Abstraction for sending transactional email.
// Modifiability: swap LoggingEmailService for a real SMTP/SES implementation without changing callers.
// Confidentiality: method signature intentionally avoids exposing raw reset tokens in callers.
namespace LooseNotes.Services;

/// <summary>Sends transactional emails. In development, implementations log instead of sending.</summary>
public interface IEmailService
{
    /// <summary>Sends a password reset link to the specified address.</summary>
    /// <param name="toEmail">Recipient email address.</param>
    /// <param name="resetLink">Full absolute URL for resetting the password.</param>
    Task SendPasswordResetAsync(string toEmail, string resetLink);
}
