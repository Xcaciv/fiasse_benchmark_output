using LooseNotes.Models;

namespace LooseNotes.ViewModels.Admin;

public class UserListViewModel
{
    public string UserId { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public int NoteCount { get; set; }
    public IList<string> Roles { get; set; } = new List<string>();
}
