using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class Rating
{
    public int Id { get; set; }

    public int NoteId { get; set; }
    public Note Note { get; set; } = null!;

    [Required]
    public string RaterId { get; set; } = string.Empty;
    public ApplicationUser Rater { get; set; } = null!;

    [Range(1, 5)]
    public int Stars { get; set; }

    [MaxLength(500)]
    public string? Comment { get; set; }

    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;
}
