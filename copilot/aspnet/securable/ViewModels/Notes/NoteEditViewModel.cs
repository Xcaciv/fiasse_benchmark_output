using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Http;

namespace LooseNotes.ViewModels.Notes;

public sealed class NoteEditViewModel
{
    public int? Id { get; set; }

    [Required]
    [StringLength(120)]
    public string Title { get; set; } = string.Empty;

    [Required]
    [StringLength(20000)]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Public note")]
    public bool IsPublic { get; set; }

    [Display(Name = "Attachments")]
    public List<IFormFile> NewAttachments { get; set; } = new();

    public IReadOnlyList<AttachmentDisplayViewModel> ExistingAttachments { get; set; } = Array.Empty<AttachmentDisplayViewModel>();
    public List<int> RemoveAttachmentIds { get; set; } = new();
}
