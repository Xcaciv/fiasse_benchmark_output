namespace LooseNotes.Services;

/// <summary>
/// Development/test email service — writes to log file instead of sending real mail.
/// Replace with SMTP/SendGrid implementation for production.
/// IMPORTANT: logs recipient address but never the reset link/token (Confidentiality).
/// </summary>
public class LoggingEmailService : IEmailService
{
    private readonly ILogger<LoggingEmailService> _logger;

    public LoggingEmailService(ILogger<LoggingEmailService> logger)
    {
        _logger = logger;
    }

    public Task SendPasswordResetAsync(string toEmail, string resetLink, CancellationToken cancellationToken = default)
    {
        // Log recipient only — never the token-bearing link (Confidentiality)
        _logger.LogInformation(
            "PASSWORD_RESET_EMAIL recipient={Recipient} [link withheld from logs]",
            toEmail);

        // In development, write the link to a separate dev-only sink or console
        // so developers can test the flow without a real mail server.
        // Wrap in #if DEBUG to prevent accidental use in production builds.
#if DEBUG
        Console.WriteLine($"[DEV] Password reset link for {toEmail}: {resetLink}");
#endif
        return Task.CompletedTask;
    }
}
