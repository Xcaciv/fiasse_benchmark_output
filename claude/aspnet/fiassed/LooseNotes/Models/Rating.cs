using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Note rating (1-5 stars). Value is validated server-side; client-supplied values outside 1-5 are rejected.
/// RaterId is taken from the authenticated session, never from request body.
/// </summary>
public sealed class Rating
{
    public int Id { get; set; }
    public int NoteId { get; set; }
    public string RaterId { get; set; } = string.Empty;

    /// <summary>
    /// Valid range 1-5. Enforced at service layer before database insert.
    /// </summary>
    [Range(1, 5)]
    public int Value { get; set; }

    public string? Comment { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Navigation properties
    public Note Note { get; set; } = null!;
    public ApplicationUser Rater { get; set; } = null!;
}
