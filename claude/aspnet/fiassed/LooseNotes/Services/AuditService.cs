using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Services;

/// <summary>
/// Writes structured audit log entries to the database.
/// Log entries include: who, what, where, when, outcome, resource (ASVS V16.2.1).
/// Sensitive values (passwords, tokens) must never be passed to this service.
/// </summary>
public sealed class AuditService : IAuditService
{
    private readonly ApplicationDbContext _db;
    private readonly ILogger<AuditService> _logger;

    public AuditService(ApplicationDbContext db, ILogger<AuditService> logger)
    {
        _db = db;
        _logger = logger;
    }

    public async Task LogAsync(
        string eventType,
        string? userId,
        string? username,
        string? sourceIp,
        string? outcome = "success",
        string? resourceType = null,
        string? resourceId = null,
        string? details = null)
    {
        var entry = new AuditLog
        {
            Timestamp = DateTime.UtcNow,
            EventType = eventType,
            UserId = userId,
            Username = username,
            SourceIp = sourceIp,
            Outcome = outcome,
            ResourceType = resourceType,
            ResourceId = resourceId,
            Details = details
        };

        _db.AuditLogs.Add(entry);

        try
        {
            await _db.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            // Audit log write failure is logged but must not crash the primary operation.
            // Secondary failure handling: emit to structured logger as fallback.
            _logger.LogError(ex,
                "Audit log write failed. Event={EventType} UserId={UserId} Outcome={Outcome}",
                eventType, userId, outcome);
        }
    }
}
