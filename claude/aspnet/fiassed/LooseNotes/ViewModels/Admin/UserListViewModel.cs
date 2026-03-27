namespace LooseNotes.ViewModels.Admin;

public sealed class UserListViewModel
{
    public IList<UserSummaryViewModel> Users { get; set; } = [];
    public string? SearchQuery { get; set; }
    public int CurrentPage { get; set; } = 1;
    public int TotalPages { get; set; }
    public int TotalCount { get; set; }
}

/// <summary>
/// Admin user summary. Excludes: password hash, reset tokens, session IDs (Confidentiality).
/// Includes only: username, email, registration date, note count, role (ASVS V13 admin requirements).
/// </summary>
public sealed class UserSummaryViewModel
{
    public string Id { get; set; } = string.Empty;
    public string Username { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime? LastLoginAt { get; set; }
    public int NoteCount { get; set; }
    public bool IsAdmin { get; set; }
}
