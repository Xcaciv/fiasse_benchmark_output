using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Services;

/// <summary>
/// Persists audit records to the database.
/// Never logs request detail directly — callers responsible for sanitization (Accountability).
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

    /// <inheritdoc />
    public async Task LogAsync(string action, string? userId, string? detail, string? ipAddress)
    {
        ArgumentNullException.ThrowIfNull(action);

        var entry = new ActivityLog
        {
            Action = action[..Math.Min(action.Length, 100)],
            UserId = userId,
            Detail = detail?[..Math.Min(detail.Length, 500)],
            IpAddress = ipAddress?[..Math.Min(ipAddress.Length, 45)],
            Timestamp = DateTime.UtcNow
        };

        _db.ActivityLogs.Add(entry);

        try
        {
            await _db.SaveChangesAsync();
        }
        catch (DbUpdateException ex)
        {
            // Audit failure must not crash the caller (Resilience)
            _logger.LogError(ex, "Failed to persist audit log entry for action {Action}", action);
        }
    }
}
