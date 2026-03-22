using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace LooseNotes.Models;

/// <summary>
/// Unforgeable share link for a note.
/// SSEM: Token is a cryptographically random GUID (128-bit entropy) – not sequential IDs.
/// </summary>
public class ShareLink
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    [ForeignKey(nameof(NoteId))]
    public Note Note { get; set; } = null!;

    /// <summary>Cryptographically random token used in the share URL.</summary>
    [Required, MaxLength(64)]
    public string Token { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public bool IsRevoked { get; set; } = false;
}
