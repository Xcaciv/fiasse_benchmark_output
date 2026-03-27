namespace LooseNotes.Models;

public class Note
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public string OwnerId { get; set; } = string.Empty;
    public bool IsPublic { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public ApplicationUser? Owner { get; set; }
    public ICollection<Attachment> Attachments { get; set; } = new List<Attachment>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
    public ICollection<ShareLink> ShareLinks { get; set; } = new List<ShareLink>();
}
