using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public class EditNoteViewModel
{
    public int Id { get; set; }

    [Required]
    [StringLength(200, MinimumLength = 1)]
    [Display(Name = "Title")]
    public string Title { get; set; } = string.Empty;

    [Required]
    [Display(Name = "Content")]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; }
}
