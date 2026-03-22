namespace LooseNotes.Configuration;

/// <summary>
/// Rate-limiting window/bucket configuration (Availability).
/// Bound from "RateLimiting" section.
/// </summary>
public class RateLimitingOptions
{
    public const string SectionName = "RateLimiting";

    public int LoginWindowSeconds { get; set; } = 60;
    public int LoginMaxAttempts { get; set; } = 10;
    public int RegisterWindowSeconds { get; set; } = 60;
    public int RegisterMaxAttempts { get; set; } = 5;
}
