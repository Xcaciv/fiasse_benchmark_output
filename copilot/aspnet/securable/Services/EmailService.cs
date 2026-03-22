using LooseNotes.Services.Interfaces;

namespace LooseNotes.Services;

/// <summary>
/// Stub email service — logs intent without revealing the reset link (Confidentiality).
/// Replace with a real SMTP/SendGrid implementation in production.
/// </summary>
public class EmailService : IEmailService
{
    private readonly ILogger<EmailService> _logger;

    public EmailService(ILogger<EmailService> logger)
    {
        _logger = logger;
    }

    /// <inheritdoc />
    /// <remarks>
    /// SECURITY: The reset link is intentionally not written to the logger.
    /// In development, write the link to the console directly so it is visible
    /// during testing without polluting structured log sinks.
    /// </remarks>
    public Task SendPasswordResetEmailAsync(string email, string resetLink)
    {
        ArgumentNullException.ThrowIfNull(email);
        ArgumentNullException.ThrowIfNull(resetLink);

        // Structured log does NOT include resetLink — avoids token leakage in log aggregators
        _logger.LogInformation("Password reset email requested for {Email}", email);

        // Development convenience: print to console stdout only
        Console.WriteLine($"[DEV] Password reset link for {email}: {resetLink}");

        return Task.CompletedTask;
    }
}
