// ShareTokenService.cs — Generates cryptographically secure share tokens.
// Authenticity: uses RandomNumberGenerator (CSPRNG) — not Random or Guid.
// Confidentiality: tokens are opaque — no embedded user or note data.
using LooseNotes.Configuration;
using Microsoft.Extensions.Options;
using System.Security.Cryptography;

namespace LooseNotes.Services;

/// <summary>Generates URL-safe Base64-encoded CSPRNG tokens for share links.</summary>
public sealed class ShareTokenService : IShareTokenService
{
    private readonly int _tokenLengthBytes;

    public ShareTokenService(IOptions<SecurityOptions> options)
    {
        _tokenLengthBytes = options.Value.ShareTokenLengthBytes;
    }

    /// <inheritdoc/>
    public string GenerateToken()
    {
        // Authenticity + Integrity: 32 bytes = 256 bits of entropy; URL-safe encoding
        var bytes = RandomNumberGenerator.GetBytes(_tokenLengthBytes);
        return Convert.ToBase64String(bytes)
            .Replace('+', '-')
            .Replace('/', '_')
            .TrimEnd('=');
    }
}
