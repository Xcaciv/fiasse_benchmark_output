using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels;

public class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public List<ActivityLogEntry> RecentActivity { get; set; } = new();
}

public class ActivityLogEntry
{
    public string Message { get; set; } = string.Empty;
    public DateTime Timestamp { get; set; }
}

public class AdminUsersViewModel
{
    public string? SearchQuery { get; set; }
    public List<AdminUserItemViewModel> Users { get; set; } = new();
}

public class AdminUserItemViewModel
{
    public string Id { get; set; } = string.Empty;
    public string Username { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public int NoteCount { get; set; }
    public bool IsAdmin { get; set; }
}

public class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public string CurrentOwner { get; set; } = string.Empty;

    [Required]
    public string NewOwnerId { get; set; } = string.Empty;

    public List<AdminUserItemViewModel> Users { get; set; } = new();
}
