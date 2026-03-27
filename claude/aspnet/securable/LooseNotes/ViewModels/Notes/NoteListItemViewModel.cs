// NoteListItemViewModel.cs — Lightweight projection for list pages.
using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public sealed class NoteListItemViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Excerpt { get; set; } = string.Empty;
    public NoteVisibility Visibility { get; set; }
    public string AuthorUserName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
}
