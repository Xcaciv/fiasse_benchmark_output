using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class Note
{
    public int Id { get; set; }
    
    [Required]
    [StringLength(200)]
    public string Title { get; set; } = string.Empty;
    
    [Required]
    public string Content { get; set; } = string.Empty;
    
    public bool IsPublic { get; set; } = false;
    
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    
    public string UserId { get; set; } = string.Empty;
    public ApplicationUser User { get; set; } = null!;
    
    public ICollection<Attachment> Attachments { get; set; } = new List<Attachment>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
    public ICollection<ShareLink> ShareLinks { get; set; } = new List<ShareLink>();
    
    public double AverageRating => Ratings.Count > 0 ? Ratings.Average(r => r.Value) : 0;
}
