namespace LooseNotes.Configuration;

/// <summary>
/// Externalized security configuration - allows policy changes without code deployment (FIASSE S2.1).
/// All values have safe defaults; override in appsettings.json.
/// </summary>
public sealed class SecurityOptions
{
    public const string SectionName = "Security";

    public int SessionTimeoutMinutes { get; init; } = 30;
    public int AbsoluteSessionLifetimeHours { get; init; } = 8;
    public int LoginRateLimitPerMinute { get; init; } = 10;
    public int SearchRateLimitPerMinute { get; init; } = 30;
    public int UploadRateLimitPerMinute { get; init; } = 20;
    public int ResetRateLimitPer15Minutes { get; init; } = 5;
    public int TopRatedRateLimitPerMinute { get; init; } = 60;
    public int RatingRateLimitPerMinute { get; init; } = 10;
}
