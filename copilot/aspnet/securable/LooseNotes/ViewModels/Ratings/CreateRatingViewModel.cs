using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Ratings;

public class CreateRatingViewModel
{
    public int NoteId { get; set; }

    [Required]
    [Range(1, 5, ErrorMessage = "Rating must be between 1 and 5.")]
    [Display(Name = "Stars")]
    public int Stars { get; set; }

    [StringLength(500)]
    [Display(Name = "Comment (optional)")]
    public string? Comment { get; set; }
}
