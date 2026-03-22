namespace LooseNotes.Services;

public class LoggingEmailService : IEmailService
{
    private readonly ILogger<LoggingEmailService> _logger;

    public LoggingEmailService(ILogger<LoggingEmailService> logger)
    {
        _logger = logger;
    }

    public Task SendPasswordResetEmailAsync(string email, string resetLink)
    {
        _logger.LogInformation("Password reset email for {Email}: {ResetLink}", email, resetLink);
        return Task.CompletedTask;
    }
}
