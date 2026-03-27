namespace LooseNotes.Services;

/// <summary>
/// Password policy validation abstraction. Shared by registration and profile update
/// so policy changes apply uniformly across all credential-setting flows (FIASSE S2.1, ASVS V6.2).
/// </summary>
public interface IPasswordValidationService
{
    /// <summary>
    /// Validates a password against the configured policy.
    /// Returns null if valid; returns an error message if invalid.
    /// </summary>
    string? Validate(string password);
}
