using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class Note
{
    public int Id { get; set; }

    [Required]
    [MaxLength(200)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    /// <summary>Default private; public notes are searchable by all users.</summary>
    public bool IsPublic { get; set; } = false;

    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;
    public DateTimeOffset UpdatedAt { get; set; } = DateTimeOffset.UtcNow;

    // Derived Integrity: OwnerId is always set server-side, never from client input.
    [Required]
    public string OwnerId { get; set; } = string.Empty;

    public ApplicationUser Owner { get; set; } = null!;

    public ICollection<Attachment> Attachments { get; set; } = new List<Attachment>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
    public ICollection<ShareLink> ShareLinks { get; set; } = new List<ShareLink>();
}
