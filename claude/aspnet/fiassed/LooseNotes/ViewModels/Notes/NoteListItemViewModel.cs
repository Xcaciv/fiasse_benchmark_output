namespace LooseNotes.ViewModels.Notes;

public sealed class NoteListItemViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Preview { get; set; } = string.Empty; // First 200 chars, output-encoded
    public bool IsPublic { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public int AttachmentCount { get; set; }
}
