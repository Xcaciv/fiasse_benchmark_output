using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Ratings;

public class RateNoteViewModel
{
    public int NoteId { get; set; }

    [Range(1, 5, ErrorMessage = "Rating must be between 1 and 5.")]
    public int Stars { get; set; }

    [MaxLength(1000)]
    public string? Comment { get; set; }
}
