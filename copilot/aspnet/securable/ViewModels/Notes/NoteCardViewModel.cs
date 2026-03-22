namespace LooseNotes.ViewModels.Notes;

public sealed class NoteCardViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Excerpt { get; set; } = string.Empty;
    public string OwnerUserName { get; set; } = string.Empty;
    public DateTime CreatedAtUtc { get; set; }
    public DateTime UpdatedAtUtc { get; set; }
    public bool IsPublic { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
}
