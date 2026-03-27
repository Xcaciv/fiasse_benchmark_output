namespace LooseNotes.Services;

public interface IEmailService
{
    Task SendPasswordResetEmailAsync(string toEmail, string resetLink, CancellationToken cancellationToken = default);
    Task SendWelcomeEmailAsync(string toEmail, string username, CancellationToken cancellationToken = default);
}
