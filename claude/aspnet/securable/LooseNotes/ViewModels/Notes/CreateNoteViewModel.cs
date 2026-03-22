using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

/// <summary>
/// Input model for note creation.
/// OwnerId is deliberately excluded — set server-side from ClaimsPrincipal (Derived Integrity).
/// </summary>
public class CreateNoteViewModel
{
    [Required, MaxLength(300)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; } = false;

    /// <summary>Optional file attachment; validated at trust boundary before storage.</summary>
    public IFormFile? Attachment { get; set; }
}
