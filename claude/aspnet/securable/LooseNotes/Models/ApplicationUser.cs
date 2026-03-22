using Microsoft.AspNetCore.Identity;
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Extended Identity user – stores application-specific profile data.
/// SSEM: PII is scoped to this model; no PII is placed on Notes or Ratings directly.
/// </summary>
public class ApplicationUser : IdentityUser
{
    [MaxLength(100)]
    public string? DisplayName { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    // Navigation
    public ICollection<Note> Notes { get; set; } = new List<Note>();
    public ICollection<Rating> Ratings { get; set; } = new List<Rating>();
}
