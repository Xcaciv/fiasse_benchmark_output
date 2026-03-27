// NoteDetailsViewModel.cs — Projection for the note detail view (read-only).
// Analyzability: only the fields the view needs are included.
using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public sealed class NoteDetailsViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public NoteVisibility Visibility { get; set; }
    public string AuthorUserName { get; set; } = string.Empty;
    public string AuthorId { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }

    public IReadOnlyList<AttachmentViewModel> Attachments { get; set; } = Array.Empty<AttachmentViewModel>();
    public IReadOnlyList<RatingViewModel> Ratings { get; set; } = Array.Empty<RatingViewModel>();

    public double AverageRating { get; set; }
    public int RatingCount { get; set; }

    /// <summary>Share token if a link exists; null if none has been generated.</summary>
    public string? ActiveShareToken { get; set; }

    /// <summary>Indicates whether the current user owns this note.</summary>
    public bool IsOwner { get; set; }

    /// <summary>The current user's existing rating value (1–5), or null if not yet rated.</summary>
    public int? CurrentUserRating { get; set; }
}

public sealed class AttachmentViewModel
{
    public int Id { get; set; }
    public string OriginalFileName { get; set; } = string.Empty;
    public long FileSizeBytes { get; set; }
    public DateTime UploadedAt { get; set; }
}

public sealed class RatingViewModel
{
    public int Id { get; set; }
    public string RaterUserName { get; set; } = string.Empty;
    public int Value { get; set; }
    public string? Comment { get; set; }
    public DateTime CreatedAt { get; set; }
}
