namespace LooseNotes.Services;

public interface IShareLinkService
{
    ShareTokenPayload CreateTokenPayload();
    string HashToken(string token);
    string RevealToken(string protectedToken);
}

public sealed record ShareTokenPayload(string Token, string TokenHash, string ProtectedToken);
