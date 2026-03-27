using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public class NoteDetailViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public string AuthorName { get; set; } = string.Empty;
    public string OwnerId { get; set; } = string.Empty;
    public bool IsPublic { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public List<Attachment> Attachments { get; set; } = new();
    public List<Rating> Ratings { get; set; } = new();
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public bool HasActiveShareLink { get; set; }
    public string? ShareToken { get; set; }
    public bool IsOwner { get; set; }
    public bool CurrentUserHasRated { get; set; }
}
