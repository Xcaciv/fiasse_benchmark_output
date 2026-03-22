using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Represents a user-authored note. IsPublic=false by default (Confidentiality).
/// Ownership enforced at service layer before any mutation (Integrity).
/// </summary>
public class Note
{
    public int Id { get; set; }

    [Required]
    [MaxLength(200)]
    public string Title { get; set; } = string.Empty;

    [Required]
    [MaxLength(10000)]
    public string Content { get; set; } = string.Empty;

    /// <summary>False by default — notes are private until explicitly made public.</summary>
    public bool IsPublic { get; set; } = false;

    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    [Required]
    public string UserId { get; set; } = string.Empty;

    public ApplicationUser? User { get; set; }
    public ICollection<Attachment> Attachments { get; set; } = new List<Attachment>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
    public ICollection<ShareLink> ShareLinks { get; set; } = new List<ShareLink>();
}
