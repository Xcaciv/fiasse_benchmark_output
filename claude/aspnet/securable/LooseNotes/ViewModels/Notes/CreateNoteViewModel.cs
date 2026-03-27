// CreateNoteViewModel.cs — Input model for creating a new note.
// Integrity: Required + MaxLength constraints at the trust boundary.
using LooseNotes.Models;
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public sealed class CreateNoteViewModel
{
    [Required]
    [StringLength(300, MinimumLength = 1)]
    public string Title { get; set; } = string.Empty;

    [Required]
    [StringLength(100000)]
    public string Content { get; set; } = string.Empty;

    public NoteVisibility Visibility { get; set; } = NoteVisibility.Private;

    /// <summary>Optional file attachment. Validated in IFileStorageService.</summary>
    public IFormFile? Attachment { get; set; }
}
