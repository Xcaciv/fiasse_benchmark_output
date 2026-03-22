using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace LooseNotes.Models;

public class Rating
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    [ForeignKey(nameof(NoteId))]
    public Note Note { get; set; } = null!;

    [Required]
    public string RaterId { get; set; } = string.Empty;

    [ForeignKey(nameof(RaterId))]
    public ApplicationUser Rater { get; set; } = null!;

    /// <summary>Star rating 1-5. Validated at the model layer to prevent out-of-range storage.</summary>
    [Range(1, 5)]
    public int Stars { get; set; }

    [MaxLength(1000)]
    public string? Comment { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
}
