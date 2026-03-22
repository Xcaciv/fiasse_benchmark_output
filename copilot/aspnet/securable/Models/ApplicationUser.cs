using Microsoft.AspNetCore.Identity;

namespace LooseNotes.Models;

/// <summary>
/// Extends IdentityUser with application-specific navigation properties.
/// Sensitive fields (email, phone) are inherited from IdentityUser and
/// never written to logs (Confidentiality).
/// </summary>
public class ApplicationUser : IdentityUser
{
    public ICollection<Note> Notes { get; set; } = new List<Note>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
    public ICollection<ActivityLog> ActivityLogs { get; set; } = new List<ActivityLog>();
}
