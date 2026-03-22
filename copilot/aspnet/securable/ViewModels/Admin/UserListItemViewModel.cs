namespace LooseNotes.ViewModels.Admin;

public sealed class UserListItemViewModel
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime RegisteredAtUtc { get; set; }
    public int NoteCount { get; set; }
}
