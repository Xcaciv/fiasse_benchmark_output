namespace LooseNotes.ViewModels.Notes;

public class NoteListItemViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string ContentPreview { get; set; } = string.Empty;
    public bool IsPublic { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public string OwnerDisplayName { get; set; } = string.Empty;
    public int AttachmentCount { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
}
