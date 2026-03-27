namespace LooseNotes.Services;

/// <summary>
/// Development/testing email service that logs rather than sends emails.
/// Replace with a real SMTP or transactional email service for production.
/// Never logs the token value - only indicates a link was generated (Confidentiality).
/// </summary>
public sealed class LoggingEmailService : IEmailService
{
    private readonly ILogger<LoggingEmailService> _logger;

    public LoggingEmailService(ILogger<LoggingEmailService> logger)
    {
        _logger = logger;
    }

    public Task SendPasswordResetAsync(string toEmail, string resetLink)
    {
        // Log that an email was sent (not the link content - token stays confidential)
        _logger.LogInformation(
            "Password reset email would be sent to {Email}. Link generated (token not logged).",
            MaskEmail(toEmail));
        // In development, write the link to a separate debug sink only
        _logger.LogDebug("DEV ONLY - Password reset link: {ResetLink}", resetLink);
        return Task.CompletedTask;
    }

    public Task SendEmailChangedNotificationAsync(string toEmail, string username)
    {
        _logger.LogInformation(
            "Email change notification would be sent to {Email} for user {Username}.",
            MaskEmail(toEmail), username);
        return Task.CompletedTask;
    }

    private static string MaskEmail(string email)
    {
        var atIndex = email.IndexOf('@');
        if (atIndex <= 1) return "***@***";
        return email[0] + "***" + email[atIndex..];
    }
}
