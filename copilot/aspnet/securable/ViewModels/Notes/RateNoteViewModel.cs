using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public class RateNoteViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;

    [Required]
    [Range(1, 5, ErrorMessage = "Stars must be between 1 and 5.")]
    [Display(Name = "Stars")]
    public int Stars { get; set; }

    [MaxLength(500, ErrorMessage = "Comment cannot exceed 500 characters.")]
    [Display(Name = "Comment (optional)")]
    public string? Comment { get; set; }

    /// <summary>Non-null when editing an existing rating.</summary>
    public int? ExistingRatingId { get; set; }
}
