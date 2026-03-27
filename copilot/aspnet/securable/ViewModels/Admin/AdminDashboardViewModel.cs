namespace LooseNotes.ViewModels.Admin;

public class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public List<string> RecentActivityLog { get; set; } = new();
}
