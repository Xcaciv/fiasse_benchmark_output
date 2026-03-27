namespace LooseNotes.Services;

public interface IAuditService
{
    void LogAuthEvent(string eventType, string userId, string details);
    void LogAdminAction(string action, string adminId, string targetDescription);
    void LogFileEvent(string eventType, string userId, string fileDescription);
}
