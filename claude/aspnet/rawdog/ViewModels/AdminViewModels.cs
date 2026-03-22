using LooseNotes.Models;
using Microsoft.AspNetCore.Mvc.Rendering;

namespace LooseNotes.ViewModels;

public class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public int TotalPublicNotes { get; set; }
    public int TotalRatings { get; set; }
    public List<ActivityLogItem> RecentActivity { get; set; } = new();
}

public class ActivityLogItem
{
    public string Description { get; set; } = string.Empty;
    public DateTime Timestamp { get; set; }
}

public class AdminUserListViewModel
{
    public string? SearchQuery { get; set; }
    public List<AdminUserItem> Users { get; set; } = new();
}

public class AdminUserItem
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public int NoteCount { get; set; }
    public List<string> Roles { get; set; } = new();
}

public class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public string CurrentOwnerId { get; set; } = string.Empty;
    public string CurrentOwnerName { get; set; } = string.Empty;
    public string NewOwnerId { get; set; } = string.Empty;
    public SelectList? Users { get; set; }
}
