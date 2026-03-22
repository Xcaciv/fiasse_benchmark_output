using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Core note entity. Visibility controls public/private access.
/// OwnerId is server-assigned on creation (Derived Integrity Principle).
/// </summary>
public class Note
{
    public int Id { get; set; }

    [Required, MaxLength(300)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    /// <summary>
    /// Server-assigned on creation; never accepted from client input.
    /// Trust boundary: controller sets this from ClaimsPrincipal, not form data.
    /// </summary>
    public string OwnerId { get; set; } = string.Empty;

    public bool IsPublic { get; set; } = false;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    public ApplicationUser? Owner { get; set; }
    public ICollection<Attachment> Attachments { get; set; } = new List<Attachment>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
    public ICollection<ShareLink> ShareLinks { get; set; } = new List<ShareLink>();
}
