using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public class NoteDetailsViewModel
{
    public Note Note { get; set; } = null!;
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public string? ActiveShareToken { get; set; }
    public bool CanEdit { get; set; }
    public bool CanDelete { get; set; }
}
