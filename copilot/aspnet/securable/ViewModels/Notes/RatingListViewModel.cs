namespace LooseNotes.ViewModels.Notes;

public sealed class RatingListViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public IReadOnlyList<RatingDisplayViewModel> Ratings { get; set; } = Array.Empty<RatingDisplayViewModel>();
}
