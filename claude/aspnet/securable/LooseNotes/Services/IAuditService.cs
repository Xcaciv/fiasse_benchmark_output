// IAuditService.cs — Accountability: contract for structured audit logging.
namespace LooseNotes.Services;

/// <summary>Records security-relevant actions for audit and compliance purposes.
/// Implementations must not log sensitive data (passwords, tokens, PII).</summary>
public interface IAuditService
{
    /// <summary>Persists an audit entry asynchronously.</summary>
    /// <param name="actorUserId">UserId of the acting user; null for unauthenticated events.</param>
    /// <param name="action">Dot-notation action name, e.g. "Note.Delete".</param>
    /// <param name="resourceType">Entity type affected, e.g. "Note".</param>
    /// <param name="resourceId">PK of affected entity (as string).</param>
    /// <param name="details">Optional safe context. Must not contain secrets.</param>
    Task LogAsync(
        string? actorUserId,
        string action,
        string? resourceType = null,
        string? resourceId = null,
        string? details = null);
}
