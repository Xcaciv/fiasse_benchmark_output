using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public sealed class EditNoteViewModel
{
    public int Id { get; set; }

    [Required]
    [MinLength(1)]
    [MaxLength(500)]
    public string Title { get; set; } = string.Empty;

    [Required]
    [MinLength(1)]
    [MaxLength(100_000)]
    public string Content { get; set; } = string.Empty;

    public bool IsPublic { get; set; }
}
