using Microsoft.AspNetCore.Identity;

namespace LooseNotes.Models;

/// <summary>
/// Extended Identity user with profile fields.
/// Username and email are the primary identity attributes.
/// </summary>
public sealed class ApplicationUser : IdentityUser
{
    public string DisplayName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime? LastLoginAt { get; set; }

    // Navigation properties
    public ICollection<Note> Notes { get; set; } = [];
    public ICollection<Rating> Ratings { get; set; } = [];
    public ICollection<AuditLog> AuditLogs { get; set; } = [];
}
