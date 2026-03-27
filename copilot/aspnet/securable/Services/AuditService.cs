namespace LooseNotes.Services;

public class AuditService : IAuditService
{
    private readonly ILogger<AuditService> _logger;

    public AuditService(ILogger<AuditService> logger)
    {
        _logger = logger;
    }

    public void LogAuthEvent(string eventType, string userId, string details)
    {
        // Never log passwords, tokens, or PII in details
        _logger.LogInformation(
            "[AUDIT:AUTH] Event={EventType} UserId={UserId} Details={Details} At={Timestamp}",
            eventType, userId, details, DateTime.UtcNow);
    }

    public void LogAdminAction(string action, string adminId, string targetDescription)
    {
        _logger.LogInformation(
            "[AUDIT:ADMIN] Action={Action} AdminId={AdminId} Target={Target} At={Timestamp}",
            action, adminId, targetDescription, DateTime.UtcNow);
    }

    public void LogFileEvent(string eventType, string userId, string fileDescription)
    {
        _logger.LogInformation(
            "[AUDIT:FILE] Event={EventType} UserId={UserId} File={FileDescription} At={Timestamp}",
            eventType, userId, fileDescription, DateTime.UtcNow);
    }
}
