namespace rawdog.ViewModels;

public sealed class AdminDashboardViewModel
{
    public string SearchTerm { get; set; } = string.Empty;

    public int TotalUsers { get; set; }

    public int TotalNotes { get; set; }

    public IReadOnlyList<AdminUserItemViewModel> Users { get; set; } = Array.Empty<AdminUserItemViewModel>();

    public IReadOnlyList<AdminActivityItemViewModel> RecentActivity { get; set; } = Array.Empty<AdminActivityItemViewModel>();

    public IReadOnlyList<AdminNoteItemViewModel> RecentNotes { get; set; } = Array.Empty<AdminNoteItemViewModel>();

    public IReadOnlyList<AdminUserOptionViewModel> UserOptions { get; set; } = Array.Empty<AdminUserOptionViewModel>();
}

public sealed class AdminUserItemViewModel
{
    public string Id { get; set; } = string.Empty;

    public string UserName { get; set; } = string.Empty;

    public string Email { get; set; } = string.Empty;

    public DateTime RegisteredAtUtc { get; set; }

    public int NoteCount { get; set; }
}

public sealed class AdminActivityItemViewModel
{
    public string ActionType { get; set; } = string.Empty;

    public string Message { get; set; } = string.Empty;

    public string UserName { get; set; } = "System";

    public DateTime CreatedAtUtc { get; set; }
}

public sealed class AdminNoteItemViewModel
{
    public int Id { get; set; }

    public string Title { get; set; } = string.Empty;

    public string OwnerId { get; set; } = string.Empty;

    public string OwnerUserName { get; set; } = string.Empty;

    public bool IsPublic { get; set; }

    public DateTime UpdatedOrCreatedAtUtc { get; set; }
}

public sealed class AdminUserOptionViewModel
{
    public string Id { get; set; } = string.Empty;

    public string UserName { get; set; } = string.Empty;
}
