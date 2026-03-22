using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Services;

/// <summary>
/// Writes security-relevant events to the AuditLog table.
/// SSEM: Passwords, raw tokens, and full PII are never passed to this service.
/// </summary>
public class AuditService : IAuditService
{
    private readonly ApplicationDbContext _db;
    private readonly ILogger<AuditService> _logger;

    public AuditService(ApplicationDbContext db, ILogger<AuditService> logger)
    {
        _db = db;
        _logger = logger;
    }

    public async Task LogAsync(string eventType, bool success, string? details = null,
        string? actorId = null, string? actorUserName = null, string? ipAddress = null)
    {
        var entry = new AuditLog
        {
            EventType = eventType,
            Success = success,
            Details = details,
            ActorId = actorId,
            ActorUserName = actorUserName,
            IpAddress = ipAddress,
            OccurredAt = DateTime.UtcNow
        };

        _db.AuditLogs.Add(entry);

        try
        {
            await _db.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            // Audit failure must not crash the main request
            _logger.LogError(ex, "Failed to persist audit log entry for event {EventType}", eventType);
        }
    }
}
