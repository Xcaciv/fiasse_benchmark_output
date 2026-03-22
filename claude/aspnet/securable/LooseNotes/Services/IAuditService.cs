namespace LooseNotes.Services;

public interface IAuditService
{
    Task LogAsync(string eventType, bool success, string? details = null,
        string? actorId = null, string? actorUserName = null, string? ipAddress = null);
}
