using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Opaque share token for unauthenticated note access.
/// Token is server-generated (cryptographically random); never client-supplied.
/// Revocable by owner; soft-deleted via IsActive flag.
/// </summary>
public class ShareLink
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    /// <summary>
    /// Base64url-encoded cryptographically random token (32 bytes = 256 bits).
    /// Generated server-side only (Derived Integrity Principle).
    /// </summary>
    [Required, MaxLength(64)]
    public string Token { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public bool IsActive { get; set; } = true;

    // Navigation
    public Note? Note { get; set; }
}
