using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

/// <summary>Display model for a single note with its attachments, ratings, and share links.</summary>
public class NoteDetailsViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public bool IsPublic { get; set; }
    public string OwnerDisplayName { get; set; } = string.Empty;
    public string OwnerId { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public IReadOnlyList<Attachment> Attachments { get; set; } = Array.Empty<Attachment>();
    public IReadOnlyList<RatingDisplayItem> Ratings { get; set; } = Array.Empty<RatingDisplayItem>();
    public double? AverageRating { get; set; }

    /// <summary>Active share link token for this note, if one exists.</summary>
    public string? ActiveShareToken { get; set; }

    /// <summary>Current user's existing rating, if any (for edit-in-place).</summary>
    public RatingInputViewModel? UserRating { get; set; }
    public bool IsOwner { get; set; }
    public bool CanRate { get; set; }
}

public class RatingDisplayItem
{
    public int Id { get; set; }
    public int Value { get; set; }
    public string? Comment { get; set; }
    public string RaterDisplayName { get; set; } = string.Empty;
    public string RaterId { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
}
