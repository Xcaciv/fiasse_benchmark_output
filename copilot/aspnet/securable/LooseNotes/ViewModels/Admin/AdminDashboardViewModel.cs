using LooseNotes.Models;

namespace LooseNotes.ViewModels.Admin;

public class AdminDashboardViewModel
{
    public int UserCount { get; set; }
    public int NoteCount { get; set; }
    public IList<AuditLog> RecentActivity { get; set; } = new List<AuditLog>();
}
