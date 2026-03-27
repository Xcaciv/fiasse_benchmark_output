// AdminDashboardViewModel.cs — Summary statistics for the admin dashboard.
namespace LooseNotes.ViewModels.Admin;

public sealed class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public IReadOnlyList<RecentAuditEntryViewModel> RecentAuditEntries { get; set; }
        = Array.Empty<RecentAuditEntryViewModel>();
}

public sealed class RecentAuditEntryViewModel
{
    public string Action { get; set; } = string.Empty;
    public string? ActorUserName { get; set; }
    public string? ResourceType { get; set; }
    public string? ResourceId { get; set; }
    public DateTime OccurredAt { get; set; }
}
