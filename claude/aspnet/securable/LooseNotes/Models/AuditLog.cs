using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// Append-only structured audit record for security-sensitive actions.
/// No sensitive data (passwords, tokens, PII beyond UserId) stored here (Accountability).
/// </summary>
public class AuditLog
{
    public long Id { get; set; }

    /// <summary>Null for anonymous/unauthenticated actions (e.g., failed login attempt).</summary>
    public string? UserId { get; set; }

    [Required, MaxLength(100)]
    public string Action { get; set; } = string.Empty;

    /// <summary>Resource type affected, e.g. "Note", "Attachment", "User".</summary>
    [MaxLength(50)]
    public string? ResourceType { get; set; }

    /// <summary>String representation of the resource primary key.</summary>
    [MaxLength(100)]
    public string? ResourceId { get; set; }

    /// <summary>
    /// Additional context as JSON. Must not contain secrets or PII.
    /// </summary>
    [MaxLength(2000)]
    public string? MetadataJson { get; set; }

    public string? IpAddress { get; set; }

    public DateTime OccurredAt { get; set; } = DateTime.UtcNow;

    public bool Succeeded { get; set; } = true;
}
