using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public class EditNoteViewModel
{
    public int Id { get; set; }

    [Required, MaxLength(300)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; }
}
