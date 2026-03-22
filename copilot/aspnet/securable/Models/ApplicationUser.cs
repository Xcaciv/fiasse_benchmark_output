using Microsoft.AspNetCore.Identity;

namespace LooseNotes.Models;

public sealed class ApplicationUser : IdentityUser
{
    public DateTime RegisteredAtUtc { get; set; } = DateTime.UtcNow;

    public ICollection<Note> OwnedNotes { get; set; } = new List<Note>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
}
