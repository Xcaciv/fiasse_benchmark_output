using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public class NoteViewModel
{
    public int Id { get; set; }

    [Required]
    [StringLength(200)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; }

    public List<IFormFile> Attachments { get; set; } = new();
}
