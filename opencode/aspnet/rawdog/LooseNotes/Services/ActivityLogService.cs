using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Services;

public interface IActivityLogService
{
    Task LogAsync(string action, string? userId = null, string? details = null, string? targetType = null, int? targetId = null);
    Task<List<ActivityLog>> GetRecentLogsAsync(int count = 50);
}

public class ActivityLogService : IActivityLogService
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<ActivityLogService> _logger;

    public ActivityLogService(ApplicationDbContext context, ILogger<ActivityLogService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task LogAsync(string action, string? userId = null, string? details = null, string? targetType = null, int? targetId = null)
    {
        var log = new ActivityLog
        {
            Action = action,
            UserId = userId,
            Details = details,
            TargetType = targetType,
            TargetId = targetId,
            CreatedAt = DateTime.UtcNow
        };

        _context.ActivityLogs.Add(log);
        await _context.SaveChangesAsync();

        _logger.LogInformation("Activity logged: {Action} by {UserId}", action, userId);
    }
}
