using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class Rating
{
    public int Id { get; set; }

    [Range(1, 5)]
    public int Value { get; set; }

    [StringLength(500)]
    public string? Comment { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public int NoteId { get; set; }

    public Note? Note { get; set; }

    public string UserId { get; set; } = string.Empty;

    public ApplicationUser? User { get; set; }
}
