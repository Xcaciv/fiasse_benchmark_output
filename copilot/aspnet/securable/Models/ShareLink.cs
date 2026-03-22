using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Provides unauthenticated read access to a note via a unique opaque token.
/// Tokens are GUIDs (32 hex chars) and can be revoked by the owner (Authenticity).
/// </summary>
public class ShareLink
{
    public int Id { get; set; }

    public int NoteId { get; set; }
    public Note? Note { get; set; }

    /// <summary>Opaque share token — Guid.NewGuid().ToString("N") at creation.</summary>
    [Required]
    [MaxLength(64)]
    public string Token { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; }

    /// <summary>Null means no expiry.</summary>
    public DateTime? ExpiresAt { get; set; }

    public bool IsRevoked { get; set; } = false;
}
