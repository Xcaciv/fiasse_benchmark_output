using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Services;

/// <summary>
/// Persists audit entries to the database.
/// Structured logging companion writes to Serilog (dual sink: DB + log file).
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

    /// <summary>
    /// Records an audit event. Non-throwing — failures are logged but not propagated
    /// to prevent audit path from disrupting the primary request (Resilience).
    /// </summary>
    public async Task RecordAsync(
        string action,
        string? userId = null,
        string? resourceType = null,
        string? resourceId = null,
        bool succeeded = true,
        string? metadataJson = null,
        string? ipAddress = null)
    {
        // Structured log first — fastest path and survives DB failures (Availability)
        _logger.LogInformation(
            "AUDIT action={Action} userId={UserId} resource={ResourceType}/{ResourceId} succeeded={Succeeded}",
            action, userId ?? "anonymous", resourceType, resourceId, succeeded);

        try
        {
            var entry = new AuditLog
            {
                Action = action,
                UserId = userId,
                ResourceType = resourceType,
                ResourceId = resourceId,
                Succeeded = succeeded,
                MetadataJson = metadataJson,
                IpAddress = ipAddress,
                OccurredAt = DateTime.UtcNow
            };

            _db.AuditLogs.Add(entry);
            await _db.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            // Audit write failure must not block the primary operation (Resilience, Availability)
            _logger.LogError(ex, "Failed to persist audit entry for action={Action}", action);
        }
    }
}
