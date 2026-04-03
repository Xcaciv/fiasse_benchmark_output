namespace LooseNotes.Services;

public class LoggingEmailService : IEmailService
{
    private readonly ILogger<LoggingEmailService> _logger;

    public LoggingEmailService(ILogger<LoggingEmailService> logger)
    {
        _logger = logger;
    }

    public Task SendEmailAsync(string to, string subject, string body)
    {
        _logger.LogInformation("Email to {To}, Subject: {Subject}, Body: {Body}", to, subject, body);
        return Task.CompletedTask;
    }
}
