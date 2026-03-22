using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Admin;

public class UserListViewModel
{
    [MaxLength(200)]
    public string? SearchQuery { get; set; }

    public IReadOnlyList<UserListItem> Users { get; set; } = Array.Empty<UserListItem>();
}

public class UserListItem
{
    public string Id { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public int NoteCount { get; set; }
    public IReadOnlyList<string> Roles { get; set; } = Array.Empty<string>();
}
