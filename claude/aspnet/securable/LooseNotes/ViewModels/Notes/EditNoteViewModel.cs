// EditNoteViewModel.cs — Input model for editing an existing note.
// Integrity: Id is bound from route, not trust-boundary form data, for IDOR prevention.
using LooseNotes.Models;
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public sealed class EditNoteViewModel
{
    public int Id { get; set; }

    [Required]
    [StringLength(300, MinimumLength = 1)]
    public string Title { get; set; } = string.Empty;

    [Required]
    [StringLength(100000)]
    public string Content { get; set; } = string.Empty;

    public NoteVisibility Visibility { get; set; }

    /// <summary>Optional new attachment to add.</summary>
    public IFormFile? NewAttachment { get; set; }
}
