// ApplicationUser.cs — Identity entity extending IdentityUser
// Analyzability: minimal surface area; only domain-relevant properties added.
using Microsoft.AspNetCore.Identity;

namespace LooseNotes.Models;

/// <summary>Extended application user. Sensitive fields (password hash, etc.)
/// are managed by IdentityUser — never surfaced in view models.</summary>
public sealed class ApplicationUser : IdentityUser
{
    /// <summary>UTC timestamp of account creation.</summary>
    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;

    /// <summary>Navigation: notes owned by this user.</summary>
    public ICollection<Note> Notes { get; init; } = new List<Note>();

    /// <summary>Navigation: ratings submitted by this user.</summary>
    public ICollection<Rating> Ratings { get; init; } = new List<Rating>();

    /// <summary>Navigation: audit entries for this user.</summary>
    public ICollection<AuditLog> AuditLogs { get; init; } = new List<AuditLog>();
}
