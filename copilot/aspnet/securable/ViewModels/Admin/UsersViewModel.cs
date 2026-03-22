namespace LooseNotes.ViewModels.Admin;

public sealed class UsersViewModel
{
    public string? Query { get; set; }
    public IReadOnlyList<UserListItemViewModel> Users { get; set; } = Array.Empty<UserListItemViewModel>();
}
