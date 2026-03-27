// SecurityOptions.cs — Strongly-typed security tuning parameters.
// Modifiability: all security thresholds are externalised — no magic numbers in code.
namespace LooseNotes.Configuration;

/// <summary>Security-related tunables bound from the "Security" config section.</summary>
public sealed class SecurityOptions
{
    public const string SectionName = "Security";

    /// <summary>Byte length of generated share tokens (cryptographically random).</summary>
    public int ShareTokenLengthBytes { get; set; } = 32;

    /// <summary>How long password reset tokens are valid (hours).</summary>
    public int PasswordResetTokenLifespanHours { get; set; } = 1;

    /// <summary>Maximum consecutive failed login attempts before lockout.</summary>
    public int MaxLoginAttempts { get; set; } = 5;

    /// <summary>Account lockout duration in minutes.</summary>
    public int LockoutDurationMinutes { get; set; } = 15;
}
