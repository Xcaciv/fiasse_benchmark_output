using System.ComponentModel.DataAnnotations;
using LooseNotes.Models;

namespace LooseNotes.ViewModels;

public class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public List<AdminUserRow> Users { get; set; } = new();
    public string? SearchQuery { get; set; }
}

public class AdminUserRow
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public int NoteCount { get; set; }
}

public class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public string CurrentOwnerName { get; set; } = string.Empty;

    [Required]
    public string NewOwnerId { get; set; } = string.Empty;

    public List<ApplicationUser> AllUsers { get; set; } = new();
}
