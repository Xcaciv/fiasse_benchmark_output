using LooseNotes.Models;

namespace LooseNotes.ViewModels.Admin;

public class DashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public IReadOnlyList<AuditLog> RecentActivity { get; set; } = Array.Empty<AuditLog>();
}
