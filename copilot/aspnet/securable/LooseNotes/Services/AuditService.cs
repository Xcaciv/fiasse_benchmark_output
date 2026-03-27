using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Services;

public class AuditService : IAuditService
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<AuditService> _logger;

    public AuditService(ApplicationDbContext context, ILogger<AuditService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task LogAsync(
        string? userId,
        string action,
        string? entityType = null,
        string? entityId = null,
        string? details = null,
        string? ipAddress = null)
    {
        var entry = new AuditLog
        {
            UserId = userId,
            Action = action,
            EntityType = entityType,
            EntityId = entityId,
            Details = details,
            IpAddress = ipAddress,
            Timestamp = DateTimeOffset.UtcNow
        };

        _context.AuditLogs.Add(entry);

        try
        {
            await _context.SaveChangesAsync();
        }
        catch (Exception ex)
        {
            // Resilience: audit failure must not disrupt the primary request flow
            _logger.LogError(ex, "Audit log persistence failed for action {Action}", action);
        }
    }
}
