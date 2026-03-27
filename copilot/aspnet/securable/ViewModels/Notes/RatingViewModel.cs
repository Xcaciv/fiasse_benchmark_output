using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public class RatingViewModel
{
    [Required]
    public int NoteId { get; set; }

    [Required]
    [Range(1, 5, ErrorMessage = "Rating must be between 1 and 5.")]
    [Display(Name = "Rating")]
    public int Value { get; set; }

    [StringLength(1000)]
    [Display(Name = "Comment")]
    public string? Comment { get; set; }
}
