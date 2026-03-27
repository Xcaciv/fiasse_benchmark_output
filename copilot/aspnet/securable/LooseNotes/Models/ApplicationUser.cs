using Microsoft.AspNetCore.Identity;

namespace LooseNotes.Models;

public class ApplicationUser : IdentityUser
{
    public string DisplayName { get; set; } = string.Empty;
    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    public ICollection<Note> Notes { get; set; } = new List<Note>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
}
