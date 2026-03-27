using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Append-only audit trail for security-sensitive actions.
/// No PII stored here — user IDs only.
/// </summary>
public class AuditLog
{
    public long Id { get; set; }

    /// <summary>Identity of the acting user; null for anonymous/system actions.</summary>
    public string? UserId { get; set; }

    [Required]
    [MaxLength(100)]
    public string Action { get; set; } = string.Empty;

    [MaxLength(100)]
    public string? EntityType { get; set; }

    [MaxLength(100)]
    public string? EntityId { get; set; }

    /// <summary>Non-sensitive contextual detail only — never log passwords, tokens, or PII.</summary>
    [MaxLength(1000)]
    public string? Details { get; set; }

    public DateTimeOffset Timestamp { get; set; } = DateTimeOffset.UtcNow;

    [MaxLength(50)]
    public string? IpAddress { get; set; }
}
