namespace LooseNotes.Services;

/// <summary>
/// Abstraction over the audit logging mechanism. Enables testing without database dependency.
/// All security-sensitive actions must route through this service (FIASSE S2.6, ASVS V16.2.1).
/// </summary>
public interface IAuditService
{
    Task LogAsync(string eventType, string? userId, string? username, string? sourceIp,
        string? outcome = "success", string? resourceType = null, string? resourceId = null,
        string? details = null);
}
