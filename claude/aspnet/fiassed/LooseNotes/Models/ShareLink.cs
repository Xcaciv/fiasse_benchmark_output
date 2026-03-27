namespace LooseNotes.Models;

/// <summary>
/// Share link for unauthenticated note access. Token is CSPRNG-generated (>=128 bits entropy).
/// Revocation is server-authoritative; revoked tokens are denied regardless of client state.
/// </summary>
public sealed class ShareLink
{
    public int Id { get; set; }
    public int NoteId { get; set; }
    public string CreatedByUserId { get; set; } = string.Empty;

    /// <summary>
    /// CSPRNG-generated token. At least 22 characters (128-bit base64url).
    /// Stored as the lookup key; never logged in plaintext.
    /// </summary>
    public string Token { get; set; } = string.Empty;

    public bool IsRevoked { get; set; } = false;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? RevokedAt { get; set; }

    // Navigation properties
    public Note Note { get; set; } = null!;
    public ApplicationUser CreatedBy { get; set; } = null!;
}
