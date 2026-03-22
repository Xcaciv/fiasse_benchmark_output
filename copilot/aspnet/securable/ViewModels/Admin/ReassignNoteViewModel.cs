using System.ComponentModel.DataAnnotations;
using LooseNotes.Models;

namespace LooseNotes.ViewModels.Admin;

public class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public string CurrentOwnerName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Please select a new owner.")]
    [Display(Name = "New Owner")]
    public string NewOwnerId { get; set; } = string.Empty;

    public IEnumerable<ApplicationUser> AvailableUsers { get; set; } = Enumerable.Empty<ApplicationUser>();
}
