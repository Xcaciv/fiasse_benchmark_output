namespace LooseNotes.ViewModels.Admin;

public sealed class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public int TotalPublicNotes { get; set; }
    public int TotalAttachments { get; set; }
    public IList<RecentAuditLogViewModel> RecentAuditLogs { get; set; } = [];
    public int CurrentPage { get; set; } = 1;
    public int TotalPages { get; set; }
}

public sealed class RecentAuditLogViewModel
{
    public long Id { get; set; }
    public DateTime Timestamp { get; set; }
    public string? Username { get; set; }
    public string EventType { get; set; } = string.Empty;
    public string? ResourceType { get; set; }
    public string? ResourceId { get; set; }
    public string? Outcome { get; set; }
    public string? SourceIp { get; set; }
    // Details excluded from admin view to avoid accidental PII exposure
}
