using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public class RatingViewModel
{
    public int NoteId { get; set; }

    [Required]
    [Range(1, 5)]
    public int Stars { get; set; }

    public string? Comment { get; set; }
}
