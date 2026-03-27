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
        _logger.LogInformation("EMAIL TO: {To} | SUBJECT: {Subject} | BODY: {Body}", to, subject, body);
        return Task.CompletedTask;
    }
}
