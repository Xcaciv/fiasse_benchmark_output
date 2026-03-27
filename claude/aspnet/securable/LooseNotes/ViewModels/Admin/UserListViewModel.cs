// UserListViewModel.cs — Projection for the admin user list.
// Confidentiality: password hashes are never included in view models.
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Admin;

public sealed class UserListViewModel
{
    [StringLength(100)]
    public string SearchQuery { get; set; } = string.Empty;

    public IReadOnlyList<UserSummaryViewModel> Users { get; set; } = Array.Empty<UserSummaryViewModel>();
}

public sealed class UserSummaryViewModel
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public int NoteCount { get; set; }
    public bool IsAdmin { get; set; }
}
