using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public sealed class RateNoteInputModel
{
    public int NoteId { get; set; }

    [Range(1, 5)]
    [Display(Name = "Stars")]
    public int Value { get; set; } = 5;

    [StringLength(1000)]
    public string? Comment { get; set; }
}
