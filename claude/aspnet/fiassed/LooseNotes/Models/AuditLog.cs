namespace LooseNotes.Models;

/// <summary>
/// Structured audit log entry. Records who did what to which resource, when, from where.
/// Sensitive values (passwords, tokens) are never stored here (FIASSE S2.6, ASVS V16.2.1).
/// This is the primary accountability mechanism for security-sensitive actions.
/// </summary>
public sealed class AuditLog
{
    public long Id { get; set; }

    /// <summary>UTC timestamp. Never use local time for audit entries.</summary>
    public DateTime Timestamp { get; set; } = DateTime.UtcNow;

    public string? UserId { get; set; }
    public string? Username { get; set; }

    /// <summary>Structured event type - enables filtering and alerting by event category.</summary>
    public string EventType { get; set; } = string.Empty;

    public string? ResourceType { get; set; }
    public string? ResourceId { get; set; }
    public string? SourceIp { get; set; }
    public string? Outcome { get; set; }

    /// <summary>Additional context. Must never contain passwords, tokens, or full PII.</summary>
    public string? Details { get; set; }

    // Navigation
    public ApplicationUser? User { get; set; }
}

/// <summary>Well-known event type constants to prevent typos and enable log filtering.</summary>
public static class AuditEventTypes
{
    public const string UserRegistered = "user.registered";
    public const string UserLoginSuccess = "user.login.success";
    public const string UserLoginFailed = "user.login.failed";
    public const string UserLogout = "user.logout";
    public const string UserPasswordChanged = "user.password.changed";
    public const string UserEmailChanged = "user.email.changed";
    public const string UserUsernameChanged = "user.username.changed";
    public const string PasswordResetRequested = "password.reset.requested";
    public const string PasswordResetCompleted = "password.reset.completed";
    public const string PasswordResetTokenReused = "password.reset.token.reused";
    public const string NoteCreated = "note.created";
    public const string NoteEdited = "note.edited";
    public const string NoteDeleted = "note.deleted";
    public const string NoteDeletedByAdmin = "note.deleted.admin";
    public const string NoteVisibilityChanged = "note.visibility.changed";
    public const string NoteOwnerReassigned = "note.owner.reassigned";
    public const string ShareLinkCreated = "sharelink.created";
    public const string ShareLinkRevoked = "sharelink.revoked";
    public const string ShareLinkAccessed = "sharelink.accessed";
    public const string FileUploaded = "file.uploaded";
    public const string FileDownloaded = "file.downloaded";
    public const string FileDeleted = "file.deleted";
    public const string RatingCreated = "rating.created";
    public const string RatingEdited = "rating.edited";
    public const string AdminUserViewed = "admin.user.viewed";
    public const string AdminUserSearched = "admin.user.searched";
    public const string AdminDashboardViewed = "admin.dashboard.viewed";
}
