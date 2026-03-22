namespace LooseNotes.Services;

/// <summary>
/// Append-only audit trail for security-sensitive actions (Accountability).
/// Callers must never pass secrets, passwords, or tokens to metadata.
/// </summary>
public interface IAuditService
{
    Task RecordAsync(
        string action,
        string? userId = null,
        string? resourceType = null,
        string? resourceId = null,
        bool succeeded = true,
        string? metadataJson = null,
        string? ipAddress = null);
}
