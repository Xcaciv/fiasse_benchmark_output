// Rating.cs — 1–5 star rating with optional comment.
// Integrity: Value is constrained via DataAnnotations and DB check constraint.
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>A star rating (1–5) and optional comment left by a user on a note.</summary>
public sealed class Rating
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    public required string UserId { get; set; }

    /// <summary>Rating value; must be 1–5. Validated at trust boundary in controller.</summary>
    [Range(1, 5)]
    public int Value { get; set; }

    /// <summary>Optional comment — max length enforced at DB level.</summary>
    [MaxLength(1000)]
    public string? Comment { get; set; }

    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // ── Navigation ────────────────────────────────────────────────────────────
    public Note? Note { get; set; }

    public ApplicationUser? User { get; set; }
}
