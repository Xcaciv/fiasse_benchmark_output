namespace LooseNotes.ViewModels.Notes;

public class NoteListItemViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Excerpt { get; set; } = string.Empty;
    public string AuthorName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public bool IsPublic { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
}
