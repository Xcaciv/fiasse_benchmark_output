using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Admin;

public class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public string CurrentOwnerId { get; set; } = string.Empty;
    public string CurrentOwnerName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Please select a new owner.")]
    public string NewOwnerId { get; set; } = string.Empty;

    public IReadOnlyList<UserOption> AllUsers { get; set; } = Array.Empty<UserOption>();
}

public class UserOption
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
}
