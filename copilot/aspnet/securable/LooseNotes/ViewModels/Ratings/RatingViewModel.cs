namespace LooseNotes.ViewModels.Ratings;

public class RatingViewModel
{
    public int Id { get; set; }
    public int NoteId { get; set; }
    public int Stars { get; set; }
    public string? Comment { get; set; }
    public string RaterDisplayName { get; set; } = string.Empty;
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public bool IsOwnRating { get; set; }
}
