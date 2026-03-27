namespace LooseNotes.Services;

/// <summary>
/// Generates and validates share link tokens. Centralizes token generation
/// so CSPRNG usage is the only implementation available (FIASSE S2.4, ASVS V7.2.3).
/// </summary>
public interface IShareTokenService
{
    /// <summary>
    /// Generates a CSPRNG token with at least 128 bits of entropy.
    /// Returns a URL-safe base64 string of at least 22 characters.
    /// </summary>
    string GenerateToken();
}
