using rawdog.Data;
using rawdog.Models;

namespace rawdog.Services;

public sealed class ActivityLogger(ApplicationDbContext dbContext, ILogger<ActivityLogger> logger) : IActivityLogger
{
    public async Task LogAsync(string actionType, string message, string? userId = null, CancellationToken cancellationToken = default)
    {
        dbContext.ActivityLogs.Add(new ActivityLog
        {
            ActionType = actionType,
            Message = message,
            UserId = userId,
            CreatedAtUtc = DateTime.UtcNow
        });

        await dbContext.SaveChangesAsync(cancellationToken);
        logger.LogInformation("Activity logged: {ActionType} - {Message}", actionType, message);
    }
}
