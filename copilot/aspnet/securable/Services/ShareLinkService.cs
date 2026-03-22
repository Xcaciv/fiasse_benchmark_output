using System.Security.Cryptography;
using System.Text;
using Microsoft.AspNetCore.DataProtection;

namespace LooseNotes.Services;

public sealed class ShareLinkService : IShareLinkService
{
    private readonly IDataProtector _protector;

    public ShareLinkService(IDataProtectionProvider dataProtectionProvider)
    {
        _protector = dataProtectionProvider.CreateProtector("LooseNotes.ShareLinks.v1");
    }

    public ShareTokenPayload CreateTokenPayload()
    {
        var bytes = RandomNumberGenerator.GetBytes(32);
        var token = Base64UrlEncode(bytes);
        return new ShareTokenPayload(token, HashToken(token), _protector.Protect(token));
    }

    public string HashToken(string token)
    {
        var hash = SHA256.HashData(Encoding.UTF8.GetBytes(token));
        return Convert.ToHexString(hash);
    }

    public string RevealToken(string protectedToken)
    {
        return _protector.Unprotect(protectedToken);
    }

    private static string Base64UrlEncode(byte[] bytes)
    {
        return Convert.ToBase64String(bytes)
            .TrimEnd('=')
            .Replace('+', '-')
            .Replace('/', '_');
    }
}
