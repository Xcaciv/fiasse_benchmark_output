namespace LooseNotes.Services;

/// <summary>
/// Records security-sensitive actions for the admin-visible audit trail.
/// Accountability: who, what, when, where — no PII or secrets in details.
/// </summary>
public interface IAuditService
{
    Task LogAsync(
        string? userId,
        string action,
        string? entityType = null,
        string? entityId = null,
        string? details = null,
        string? ipAddress = null);
}
