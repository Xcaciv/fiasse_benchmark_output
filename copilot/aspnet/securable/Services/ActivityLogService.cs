using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Services;

public sealed class ActivityLogService : IActivityLogService
{
    private readonly ApplicationDbContext _dbContext;

    public ActivityLogService(ApplicationDbContext dbContext)
    {
        _dbContext = dbContext;
    }

    public async Task LogAsync(string actionType, string description, string? actorUserId, string? ipAddress, CancellationToken cancellationToken = default)
    {
        var entry = new ActivityLog
        {
            ActionType = actionType,
            Description = description,
            ActorUserId = actorUserId,
            IpAddress = ipAddress,
            CreatedAtUtc = DateTime.UtcNow
        };

        _dbContext.ActivityLogs.Add(entry);
        await _dbContext.SaveChangesAsync(cancellationToken);
    }
}
