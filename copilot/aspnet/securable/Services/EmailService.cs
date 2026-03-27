namespace LooseNotes.Services;

// Stub email service - logs instead of sending real emails.
// Replace with real SMTP implementation for production.
public class EmailService : IEmailService
{
    private readonly ILogger<EmailService> _logger;

    public EmailService(ILogger<EmailService> logger)
    {
        _logger = logger;
    }

    public Task SendPasswordResetEmailAsync(string toEmail, string resetLink, CancellationToken cancellationToken = default)
    {
        // In production, send actual email. Log link for development only.
        _logger.LogInformation("[EMAIL] Password reset requested for {Email}. Link: {ResetLink}", toEmail, resetLink);
        return Task.CompletedTask;
    }

    public Task SendWelcomeEmailAsync(string toEmail, string username, CancellationToken cancellationToken = default)
    {
        _logger.LogInformation("[EMAIL] Welcome email sent to {Email} for user {Username}", toEmail, username);
        return Task.CompletedTask;
    }
}
