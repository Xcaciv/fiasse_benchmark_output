using System.Security.Cryptography;

namespace LooseNotes.Services;

/// <summary>
/// Generates share link tokens using CSPRNG with >= 128 bits of entropy (ASVS V7.2.3).
/// Token format: URL-safe base64, at least 22 characters.
/// Sequential IDs, timestamps, and GUIDs are explicitly rejected here as insufficient.
/// </summary>
public sealed class ShareTokenService : IShareTokenService
{
    // 32 bytes = 256 bits of CSPRNG entropy; base64url encoding yields 43 chars
    private const int TokenByteLength = 32;

    public string GenerateToken()
    {
        var bytes = RandomNumberGenerator.GetBytes(TokenByteLength);
        // Use URL-safe base64 without padding - safe for use directly in URLs
        return Convert.ToBase64String(bytes)
            .Replace('+', '-')
            .Replace('/', '_')
            .TrimEnd('=');
    }
}
