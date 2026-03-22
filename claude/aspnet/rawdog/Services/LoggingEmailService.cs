namespace LooseNotes.Services;

/// <summary>
/// Development/fallback email service that logs emails instead of sending them.
/// Replace with a real SMTP/SendGrid implementation for production.
/// </summary>
public class LoggingEmailService : IEmailService
{
    private readonly ILogger<LoggingEmailService> _logger;

    public LoggingEmailService(ILogger<LoggingEmailService> logger)
    {
        _logger = logger;
    }

    public Task SendPasswordResetEmailAsync(string toEmail, string resetLink)
    {
        _logger.LogInformation(
            "PASSWORD RESET EMAIL (not sent - configure SMTP for production)\nTo: {Email}\nLink: {Link}",
            toEmail, resetLink);
        return Task.CompletedTask;
    }
}
