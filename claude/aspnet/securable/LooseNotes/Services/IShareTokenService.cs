namespace LooseNotes.Services;

/// <summary>
/// Generates and validates cryptographically random share tokens.
/// Isolates token generation so it is centrally testable and replaceable (Modifiability).
/// </summary>
public interface IShareTokenService
{
    /// <summary>Generates a URL-safe base64-encoded cryptographically random token.</summary>
    string GenerateToken();
}
