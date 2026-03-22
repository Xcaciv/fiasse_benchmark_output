using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public class NoteEditViewModel
{
    public int Id { get; set; }

    [Required(ErrorMessage = "Title is required.")]
    [MaxLength(200, ErrorMessage = "Title cannot exceed 200 characters.")]
    [Display(Name = "Title")]
    public string Title { get; set; } = string.Empty;

    [Required(ErrorMessage = "Content is required.")]
    [MaxLength(10000, ErrorMessage = "Content cannot exceed 10,000 characters.")]
    [Display(Name = "Content")]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; }

    [Display(Name = "New Attachments")]
    public IFormFileCollection? NewAttachments { get; set; }
}
