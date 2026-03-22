using System.ComponentModel.DataAnnotations;

namespace rawdog.Models;

public sealed class NoteRating
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    public Note? Note { get; set; }

    [Required]
    public string UserId { get; set; } = string.Empty;

    public ApplicationUser? User { get; set; }

    [Range(1, 5)]
    public int Score { get; set; }

    [StringLength(1000)]
    public string? Comment { get; set; }

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAtUtc { get; set; }
}
