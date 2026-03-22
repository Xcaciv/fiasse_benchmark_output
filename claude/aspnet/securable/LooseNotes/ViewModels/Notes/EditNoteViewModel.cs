using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

/// <summary>
/// Input model for note editing.
/// Id is route-bound and ownership is verified in controller (Derived Integrity).
/// </summary>
public class EditNoteViewModel
{
    [Required]
    public int Id { get; set; }

    [Required, MaxLength(300)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; }

    public IFormFile? NewAttachment { get; set; }
}
