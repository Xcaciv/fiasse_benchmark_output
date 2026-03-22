using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Immutable audit record. Written by IAuditService; never updated or deleted.
/// UserId nullable to support unauthenticated events (Accountability).
/// </summary>
public class ActivityLog
{
    public int Id { get; set; }

    /// <summary>Nullable — allows logging anonymous/pre-auth events.</summary>
    public string? UserId { get; set; }
    public ApplicationUser? User { get; set; }

    [Required]
    [MaxLength(100)]
    public string Action { get; set; } = string.Empty;

    [MaxLength(500)]
    public string? Detail { get; set; }

    public DateTime Timestamp { get; set; }

    [MaxLength(45)]
    public string? IpAddress { get; set; }
}
