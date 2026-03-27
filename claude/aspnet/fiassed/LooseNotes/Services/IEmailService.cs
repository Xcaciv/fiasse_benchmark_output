namespace LooseNotes.Services;

/// <summary>
/// Email delivery abstraction. Decouples business logic from transport,
/// enabling testing and provider substitution without code changes (FIASSE S2.1, Modifiability).
/// </summary>
public interface IEmailService
{
    Task SendPasswordResetAsync(string toEmail, string resetLink);
    Task SendEmailChangedNotificationAsync(string toEmail, string username);
}
