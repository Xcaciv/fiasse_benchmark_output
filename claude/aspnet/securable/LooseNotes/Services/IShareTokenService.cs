// IShareTokenService.cs — Contract for cryptographically secure share token generation.
// Authenticity: tokens are opaque random values, not predictable sequences.
namespace LooseNotes.Services;

/// <summary>Generates and validates share tokens for note access links.</summary>
public interface IShareTokenService
{
    /// <summary>Generates a URL-safe, cryptographically random share token.</summary>
    string GenerateToken();
}
