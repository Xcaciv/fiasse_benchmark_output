namespace LooseNotes.Services;

public interface IEmailDispatchService
{
    Task SendPasswordResetAsync(string toEmail, string resetUrl, CancellationToken cancellationToken = default);
}
