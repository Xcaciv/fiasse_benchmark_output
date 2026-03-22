namespace LooseNotes.ViewModels.Admin;

public sealed class DashboardViewModel
{
    public int TotalUserCount { get; set; }
    public int TotalNoteCount { get; set; }
    public IReadOnlyList<ActivityLogItemViewModel> RecentActivity { get; set; } = Array.Empty<ActivityLogItemViewModel>();
}
