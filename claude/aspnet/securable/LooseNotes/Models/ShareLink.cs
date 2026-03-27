// ShareLink.cs — A unique, revocable share token for a note.
// Authenticity: token is a cryptographically random byte array (see ShareTokenService).
// Availability: tokens can be revoked (IsActive flag).
namespace LooseNotes.Models;

/// <summary>A revocable share link that grants read access to a note without authentication.</summary>
public sealed class ShareLink
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    /// <summary>URL-safe Base64-encoded cryptographically random token.
    /// Indexed for fast lookup. Never logged.</summary>
    public required string Token { get; set; }

    public bool IsActive { get; set; } = true;

    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;

    // ── Navigation ────────────────────────────────────────────────────────────
    public Note? Note { get; set; }
}
