// LoggingEmailService.cs — Development email stub that logs instead of sending.
// Confidentiality: logs the reset link (which contains a token) at Debug level
//   so it is NOT emitted in production (log level configured per environment).
namespace LooseNotes.Services;

/// <summary>Development-only email service that writes to the structured logger.
/// Replace with an SMTP or cloud email implementation for production.</summary>
public sealed class LoggingEmailService : IEmailService
{
    private readonly ILogger<LoggingEmailService> _logger;

    public LoggingEmailService(ILogger<LoggingEmailService> logger) => _logger = logger;

    /// <inheritdoc/>
    public Task SendPasswordResetAsync(string toEmail, string resetLink)
    {
        // Debug level — suppressed in production by log level config (Confidentiality)
        _logger.LogDebug(
            "DEV EMAIL | PasswordReset | To={Email} | Link={Link}",
            toEmail, resetLink);

        return Task.CompletedTask;
    }
}
