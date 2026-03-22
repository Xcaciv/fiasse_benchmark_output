namespace LooseNotes.Services.Interfaces;

/// <summary>
/// Structured audit trail. Actions are written as immutable records (Accountability).
/// Implementations MUST NOT write passwords, tokens, or PII beyond usernames.
/// </summary>
public interface IAuditService
{
    /// <summary>
    /// Persists an audit event. detail must not contain passwords or tokens.
    /// </summary>
    Task LogAsync(string action, string? userId, string? detail, string? ipAddress);
}
