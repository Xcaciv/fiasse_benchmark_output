namespace LooseNotes.ViewModels.Notes;

/// <summary>Lightweight note summary for list/search views.</summary>
public class NoteListItemViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;

    /// <summary>First 200 characters of content, HTML-stripped.</summary>
    public string Excerpt { get; set; } = string.Empty;

    public string OwnerDisplayName { get; set; } = string.Empty;
    public bool IsPublic { get; set; }
    public DateTime CreatedAt { get; set; }
    public double? AverageRating { get; set; }
    public int RatingCount { get; set; }
}
