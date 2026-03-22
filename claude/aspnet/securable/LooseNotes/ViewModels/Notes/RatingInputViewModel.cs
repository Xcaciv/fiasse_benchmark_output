using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

/// <summary>Input model for submitting or editing a rating.</summary>
public class RatingInputViewModel
{
    public int NoteId { get; set; }

    /// <summary>Existing rating id for edits; 0 for new ratings.</summary>
    public int RatingId { get; set; }

    [Required, Range(1, 5)]
    public int Value { get; set; }

    [MaxLength(1000)]
    public string? Comment { get; set; }
}
