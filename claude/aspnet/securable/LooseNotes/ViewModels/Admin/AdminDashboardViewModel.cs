namespace LooseNotes.ViewModels.Admin;

/// <summary>Dashboard summary counts for the admin overview page.</summary>
public class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public IReadOnlyList<RecentAuditItem> RecentActivity { get; set; } = Array.Empty<RecentAuditItem>();
}

public class RecentAuditItem
{
    public string Action { get; set; } = string.Empty;
    public string? UserId { get; set; }
    public string? ResourceType { get; set; }
    public string? ResourceId { get; set; }
    public bool Succeeded { get; set; }
    public DateTime OccurredAt { get; set; }
}
