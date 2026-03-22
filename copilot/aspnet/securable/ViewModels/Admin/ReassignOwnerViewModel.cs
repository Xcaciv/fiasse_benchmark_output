using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Mvc.Rendering;

namespace LooseNotes.ViewModels.Admin;

public sealed class ReassignOwnerViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public string CurrentOwnerUserName { get; set; } = string.Empty;

    [Required]
    [Display(Name = "New owner")]
    public string NewOwnerId { get; set; } = string.Empty;

    public IReadOnlyList<SelectListItem> Users { get; set; } = Array.Empty<SelectListItem>();
}
