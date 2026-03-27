// RatingInputViewModel.cs — Input model for submitting/updating a rating.
// Integrity: Range constraint enforced client-side and at trust boundary.
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public sealed class RatingInputViewModel
{
    public int NoteId { get; set; }

    [Required]
    [Range(1, 5, ErrorMessage = "Rating must be between 1 and 5.")]
    public int Value { get; set; }

    [StringLength(1000)]
    public string? Comment { get; set; }
}
