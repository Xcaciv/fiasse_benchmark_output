using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Persistent audit trail for security-relevant events.
/// SSEM: Captures who did what, when, and on which resource – no secrets stored here.
/// </summary>
public class AuditLog
{
    public long Id { get; set; }

    /// <summary>Null for anonymous events (e.g. failed login with unknown username).</summary>
    [MaxLength(450)]
    public string? ActorId { get; set; }

    [MaxLength(256)]
    public string? ActorUserName { get; set; }

    [Required, MaxLength(100)]
    public string EventType { get; set; } = string.Empty;

    [MaxLength(2000)]
    public string? Details { get; set; }

    [MaxLength(50)]
    public string? IpAddress { get; set; }

    public DateTime OccurredAt { get; set; } = DateTime.UtcNow;

    public bool Success { get; set; }
}
