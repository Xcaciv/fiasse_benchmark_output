namespace LooseNotes.ViewModels.Admin;

public class UserListItemViewModel
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime RegistrationDate { get; set; }
    public int NoteCount { get; set; }
}

public class UserListViewModel
{
    public List<UserListItemViewModel> Users { get; set; } = new();
    public string? SearchQuery { get; set; }
}
