using LooseNotes.Models;

namespace LooseNotes.ViewModels.Admin;

public class DashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public int PublicNotes { get; set; }
    public IEnumerable<ActivityLog> RecentActivity { get; set; } = new List<ActivityLog>();
}
