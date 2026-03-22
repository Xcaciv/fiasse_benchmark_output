using LooseNotes.Models;

namespace LooseNotes.ViewModels.Admin;

public class UserListViewModel
{
    public string? SearchQuery { get; set; }
    public IReadOnlyList<UserSummary> Users { get; set; } = Array.Empty<UserSummary>();
}

public class UserSummary
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public int NoteCount { get; set; }
    public bool IsAdmin { get; set; }
}
