using Microsoft.AspNetCore.Identity;

namespace rawdog.Models;

public sealed class ApplicationUser : IdentityUser
{
    public DateTime RegisteredAtUtc { get; set; } = DateTime.UtcNow;

    public ICollection<Note> Notes { get; set; } = new List<Note>();

    public ICollection<NoteRating> Ratings { get; set; } = new List<NoteRating>();
}
