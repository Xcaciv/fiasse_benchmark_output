using System.ComponentModel.DataAnnotations;

namespace rawdog.Models;

public sealed class Note
{
    public int Id { get; set; }

    [Required]
    [StringLength(200)]
    public string Title { get; set; } = string.Empty;

    [Required]
    [StringLength(20000)]
    public string Content { get; set; } = string.Empty;

    public bool IsPublic { get; set; }

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;

    public DateTime? UpdatedAtUtc { get; set; }

    [Required]
    public string OwnerId { get; set; } = string.Empty;

    public ApplicationUser? Owner { get; set; }

    public ICollection<NoteAttachment> Attachments { get; set; } = new List<NoteAttachment>();

    public ICollection<NoteRating> Ratings { get; set; } = new List<NoteRating>();

    public ICollection<NoteShareLink> ShareLinks { get; set; } = new List<NoteShareLink>();
}
