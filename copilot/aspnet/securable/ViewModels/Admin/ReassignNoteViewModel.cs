using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Admin;

public class ReassignNoteViewModel
{
    [Required]
    public int NoteId { get; set; }

    public string NoteTitle { get; set; } = string.Empty;

    [Required]
    [Display(Name = "New Owner")]
    public string NewOwnerId { get; set; } = string.Empty;

    public List<UserSelectItem> Users { get; set; } = new();
}

public class UserSelectItem
{
    public string Id { get; set; } = string.Empty;
    public string DisplayText { get; set; } = string.Empty;
}
