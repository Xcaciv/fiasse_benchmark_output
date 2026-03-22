using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public sealed class Rating
{
    public int Id { get; set; }

    public int NoteId { get; set; }
    public Note Note { get; set; } = null!;

    [Required]
    public string UserId { get; set; } = string.Empty;
    public ApplicationUser User { get; set; } = null!;

    [Range(1, 5)]
    public int Value { get; set; }

    [StringLength(1000)]
    public string? Comment { get; set; }

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAtUtc { get; set; } = DateTime.UtcNow;
}
