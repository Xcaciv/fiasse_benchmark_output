using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Star rating (1–5) with optional comment. One rating per user per note
/// enforced via unique index (Integrity).
/// </summary>
public class Rating
{
    public int Id { get; set; }

    public int NoteId { get; set; }
    public Note? Note { get; set; }

    [Required]
    public string UserId { get; set; } = string.Empty;
    public ApplicationUser? User { get; set; }

    [Range(1, 5)]
    public int Stars { get; set; }

    [MaxLength(500)]
    public string? Comment { get; set; }

    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
}
