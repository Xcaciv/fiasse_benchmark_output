namespace LooseNotes.ViewModels.Notes;

public sealed class TopRatedNoteViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Preview { get; set; } = string.Empty; // First 200 chars, output-encoded in view
    public string AuthorUsername { get; set; } = string.Empty; // Username only; no user ID or email
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
}
