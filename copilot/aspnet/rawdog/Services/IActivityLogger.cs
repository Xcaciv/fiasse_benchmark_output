namespace rawdog.Services;

public interface IActivityLogger
{
    Task LogAsync(string actionType, string message, string? userId = null, CancellationToken cancellationToken = default);
}
