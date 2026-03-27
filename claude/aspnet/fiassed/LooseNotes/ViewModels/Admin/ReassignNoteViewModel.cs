using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Admin;

public sealed class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    public string CurrentTitle { get; set; } = string.Empty;
    public string CurrentOwnerUsername { get; set; } = string.Empty;

    [Required(ErrorMessage = "Target username is required.")]
    [MaxLength(50)]
    public string TargetUsername { get; set; } = string.Empty;
}
