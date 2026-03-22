using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// User rating (1–5) for a note, with optional comment.
/// One rating per user per note enforced at DB and service level.
/// </summary>
public class Rating
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    /// <summary>Server-assigned from ClaimsPrincipal (Derived Integrity).</summary>
    public string RaterId { get; set; } = string.Empty;

    /// <summary>Valid range 1–5; validated at trust boundary before persist.</summary>
    [Range(1, 5)]
    public int Value { get; set; }

    [MaxLength(1000)]
    public string? Comment { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    public Note? Note { get; set; }
    public ApplicationUser? Rater { get; set; }
}
