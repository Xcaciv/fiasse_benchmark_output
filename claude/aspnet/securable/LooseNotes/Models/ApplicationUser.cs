using Microsoft.AspNetCore.Identity;

namespace LooseNotes.Models;

/// <summary>
/// Extended Identity user — adds display name and navigation to owned notes.
/// Personal data is annotated for GDPR/compliance tooling (Confidentiality).
/// </summary>
public class ApplicationUser : IdentityUser
{
    /// <summary>Public-facing display name. Not used for authentication.</summary>
    [PersonalData]
    public string DisplayName { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    // Navigation properties (EF Core)
    public ICollection<Note> Notes { get; set; } = new List<Note>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
}
