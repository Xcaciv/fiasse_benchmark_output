namespace LooseNotes.Configuration;

/// <summary>
/// Security-related configuration values.
/// Bound from "Security" section in appsettings.json.
/// </summary>
public class SecurityOptions
{
    public const string SectionName = "Security";

    /// <summary>Password reset token validity in minutes. Default: 60.</summary>
    public int PasswordResetTokenLifetimeMinutes { get; set; } = 60;

    /// <summary>Byte length for cryptographically random share tokens. Default: 32 (256 bits).</summary>
    public int ShareTokenSizeBytes { get; set; } = 32;
}
