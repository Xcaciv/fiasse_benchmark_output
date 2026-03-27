using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

/// <summary>
/// Rating input. RaterId is NOT included - taken from authenticated session (Derived Integrity).
/// Value is validated server-side even though Range attribute is present (annotations are UX only).
/// </summary>
public sealed class RatingInputViewModel
{
    public int NoteId { get; set; }

    [Required]
    [Range(1, 5, ErrorMessage = "Rating must be between 1 and 5.")]
    public int Value { get; set; }

    [MaxLength(1000)]
    public string? Comment { get; set; }

    public int? ExistingRatingId { get; set; }
}
