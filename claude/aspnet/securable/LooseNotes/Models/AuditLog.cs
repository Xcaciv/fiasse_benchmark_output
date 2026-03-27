// AuditLog.cs — Immutable structured audit trail entry.
// Accountability: captures WHO did WHAT to WHICH resource and WHEN.
// Confidentiality: no passwords, tokens, or PII stored in Details.
namespace LooseNotes.Models;

/// <summary>Immutable audit record. All security-relevant actions produce an entry.</summary>
public sealed class AuditLog
{
    public int Id { get; set; }

    /// <summary>UserId of the actor; nullable for unauthenticated events.</summary>
    public string? ActorUserId { get; set; }

    /// <summary>Human-readable action name (e.g. "Note.Delete", "User.Login.Failed").</summary>
    public required string Action { get; set; }

    /// <summary>Resource type involved (e.g. "Note", "User", "ShareLink").</summary>
    public string? ResourceType { get; set; }

    /// <summary>Primary key of the affected resource (as string for flexibility).</summary>
    public string? ResourceId { get; set; }

    /// <summary>Additional context — must NOT contain secrets, passwords, or tokens.</summary>
    public string? Details { get; set; }

    /// <summary>Client IP address for correlation (from X-Forwarded-For or RemoteIp).</summary>
    public string? IpAddress { get; set; }

    public DateTime OccurredAt { get; init; } = DateTime.UtcNow;

    // ── Navigation ────────────────────────────────────────────────────────────
    public ApplicationUser? Actor { get; set; }
}
