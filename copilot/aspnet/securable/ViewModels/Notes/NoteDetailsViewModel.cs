namespace LooseNotes.ViewModels.Notes;

public sealed class NoteDetailsViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public bool IsPublic { get; set; }
    public string OwnerUserName { get; set; } = string.Empty;
    public DateTime CreatedAtUtc { get; set; }
    public DateTime UpdatedAtUtc { get; set; }
    public IReadOnlyList<AttachmentDisplayViewModel> Attachments { get; set; } = Array.Empty<AttachmentDisplayViewModel>();
    public IReadOnlyList<RatingDisplayViewModel> Ratings { get; set; } = Array.Empty<RatingDisplayViewModel>();
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public bool CanEdit { get; set; }
    public bool CanDelete { get; set; }
    public bool CanManageShares { get; set; }
    public bool CanViewRatings { get; set; }
    public bool CanRate { get; set; }
    public bool IsSharedView { get; set; }
    public RateNoteInputModel RatingForm { get; set; } = new();
}
