using LooseNotes.Models;

namespace LooseNotes.ViewModels.Admin;

public class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public IEnumerable<ActivityLog> RecentActivity { get; set; } = Enumerable.Empty<ActivityLog>();
}
