using LooseNotes.Models;

namespace LooseNotes.ViewModels.Admin;

public class UserListViewModel
{
    public IEnumerable<ApplicationUser> Users { get; set; } = Enumerable.Empty<ApplicationUser>();
    public string? SearchQuery { get; set; }
}
