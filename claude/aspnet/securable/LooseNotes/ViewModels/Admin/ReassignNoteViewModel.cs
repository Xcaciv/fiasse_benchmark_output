using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Admin;

public class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public string CurrentOwnerDisplayName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Please select a new owner.")]
    [Display(Name = "New Owner")]
    public string NewOwnerEmail { get; set; } = string.Empty;

    /// <summary>All user emails available for the dropdown selection.</summary>
    public IReadOnlyList<string> AvailableUserEmails { get; set; } = Array.Empty<string>();
}
