namespace LooseNotes.Services;

public interface IEmailService
{
    Task SendPasswordResetEmailAsync(string email, string resetLink);
}
