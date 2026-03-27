namespace LooseNotes.ViewModels.Notes;

public sealed class NoteDetailsViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public bool IsPublic { get; set; }
    public string OwnerUsername { get; set; } = string.Empty;
    public bool IsOwner { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public IList<AttachmentViewModel> Attachments { get; set; } = [];
    public IList<RatingViewModel> Ratings { get; set; } = [];
    public string? ActiveShareToken { get; set; }
}

public sealed class AttachmentViewModel
{
    public int Id { get; set; }
    public string OriginalFileName { get; set; } = string.Empty; // Display only
    public string ContentType { get; set; } = string.Empty;
    public long FileSizeBytes { get; set; }
    public DateTime UploadedAt { get; set; }
}

public sealed class RatingViewModel
{
    public int Id { get; set; }
    public int Value { get; set; }
    public string? Comment { get; set; }
    public string RaterUsername { get; set; } = string.Empty; // Username only; no email (FIASSE S2.3)
    public bool IsCurrentUser { get; set; }
    public DateTime CreatedAt { get; set; }
}
