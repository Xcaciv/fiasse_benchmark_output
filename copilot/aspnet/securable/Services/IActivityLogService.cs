namespace LooseNotes.Services;

public interface IActivityLogService
{
    Task LogAsync(string actionType, string description, string? actorUserId, string? ipAddress, CancellationToken cancellationToken = default);
}
