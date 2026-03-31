using Microsoft.AspNetCore.Identity;

namespace LooseNotes.Models;

public class ApplicationUser : IdentityUser
{
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? LastLoginAt { get; set; }
    
    public virtual ICollection<Note> Notes { get; set; } = new List<Note>();
    public virtual ICollection<Rating> Ratings { get; set; } = new List<Rating>();
}
