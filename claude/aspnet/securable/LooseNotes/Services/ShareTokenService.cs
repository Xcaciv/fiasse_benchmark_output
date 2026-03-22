using LooseNotes.Configuration;
using Microsoft.Extensions.Options;
using System.Security.Cryptography;

namespace LooseNotes.Services;

/// <summary>
/// Generates URL-safe cryptographically random share link tokens using RNGCryptoServiceProvider.
/// 32 bytes (256 bits) entropy by default (Authenticity, Integrity).
/// </summary>
public class ShareTokenService : IShareTokenService
{
    private readonly int _tokenSizeBytes;

    public ShareTokenService(IOptions<SecurityOptions> options)
    {
        _tokenSizeBytes = options.Value.ShareTokenSizeBytes;
    }

    public string GenerateToken()
    {
        var bytes = RandomNumberGenerator.GetBytes(_tokenSizeBytes);
        // Base64url encoding: URL-safe and compact
        return Convert.ToBase64String(bytes)
                       .TrimEnd('=')
                       .Replace('+', '-')
                       .Replace('/', '_');
    }
}
