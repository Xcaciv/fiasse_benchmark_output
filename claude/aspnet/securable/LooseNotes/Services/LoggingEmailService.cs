namespace LooseNotes.Services;

/// <summary>
/// Development/stub email service that logs the reset link instead of sending email.
/// Replace with an SMTP or transactional-email implementation in production.
/// SSEM: The reset link is logged at Information level so developers can use it during testing.
///       In production replace with real SMTP; remove link from logs.
/// </summary>
public class LoggingEmailService : IEmailService
{
    private readonly ILogger<LoggingEmailService> _logger;

    public LoggingEmailService(ILogger<LoggingEmailService> logger)
        => _logger = logger;

    public Task SendPasswordResetAsync(string toEmail, string resetLink)
    {
        // SSEM: Log only the link (no token in raw form) and the destination address
        _logger.LogInformation(
            "[DEV] Password reset link for {Email}: {ResetLink}", toEmail, resetLink);
        return Task.CompletedTask;
    }
}
