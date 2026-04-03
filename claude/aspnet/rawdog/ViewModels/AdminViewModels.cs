using LooseNotes.Models;

namespace LooseNotes.ViewModels;

public class AdminDashboardViewModel
{
    public int UserCount { get; set; }
    public int NoteCount { get; set; }
    public string? Command { get; set; }
    public string? CommandOutput { get; set; }
}

public class UserListViewModel
{
    public List<ApplicationUser> Users { get; set; } = new();
}

public class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    public int TargetUserId { get; set; }
    public Note? Note { get; set; }
    public List<ApplicationUser> Users { get; set; } = new();
}

public class DatabaseConfigViewModel
{
    public string ConnectionString { get; set; } = string.Empty;
    public string? Message { get; set; }
}
