using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

/// <summary>
/// Note creation input. UserId is NOT included here; it is taken from the authenticated session.
/// IsPublic is NOT included; visibility defaults to private on the server (Derived Integrity Principle).
/// </summary>
public sealed class CreateNoteViewModel
{
    [Required]
    [MinLength(1)]
    [MaxLength(500)]
    public string Title { get; set; } = string.Empty;

    [Required]
    [MinLength(1)]
    [MaxLength(100_000)]
    public string Content { get; set; } = string.Empty;
}
