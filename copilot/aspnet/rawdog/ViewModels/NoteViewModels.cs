using System.ComponentModel.DataAnnotations;
using Microsoft.AspNetCore.Http;

namespace rawdog.ViewModels;

public sealed class NoteListItemViewModel
{
    public int Id { get; set; }

    public string Title { get; set; } = string.Empty;

    public bool IsPublic { get; set; }

    public DateTime CreatedAtUtc { get; set; }

    public DateTime? UpdatedAtUtc { get; set; }

    public int AttachmentCount { get; set; }

    public int RatingCount { get; set; }

    public double AverageRating { get; set; }
}

public sealed class NoteIndexViewModel
{
    public IReadOnlyList<NoteListItemViewModel> Notes { get; set; } = Array.Empty<NoteListItemViewModel>();
}

public sealed class NoteFormViewModel
{
    public int? Id { get; set; }

    [Required]
    [StringLength(200)]
    public string Title { get; set; } = string.Empty;

    [Required]
    [StringLength(20000)]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Make this note public")]
    public bool IsPublic { get; set; }

    [Display(Name = "Add attachments")]
    public List<IFormFile> UploadedFiles { get; set; } = new();

    public IReadOnlyList<NoteAttachmentItemViewModel> ExistingAttachments { get; set; } = Array.Empty<NoteAttachmentItemViewModel>();
}

public sealed class NoteAttachmentItemViewModel
{
    public int Id { get; set; }

    public string OriginalFileName { get; set; } = string.Empty;

    public string ContentType { get; set; } = string.Empty;

    public long SizeBytes { get; set; }
}

public sealed class NoteRatingItemViewModel
{
    public int Score { get; set; }

    public string? Comment { get; set; }

    public string UserName { get; set; } = string.Empty;

    public DateTime CreatedAtUtc { get; set; }
}

public sealed class NoteRatingInputViewModel
{
    [Range(1, 5)]
    public int Score { get; set; }

    [StringLength(1000)]
    public string? Comment { get; set; }
}

public sealed class NoteDetailsViewModel
{
    public int Id { get; set; }

    public string Title { get; set; } = string.Empty;

    public string Content { get; set; } = string.Empty;

    public string Author { get; set; } = string.Empty;

    public bool IsPublic { get; set; }

    public DateTime CreatedAtUtc { get; set; }

    public DateTime? UpdatedAtUtc { get; set; }

    public IReadOnlyList<NoteAttachmentItemViewModel> Attachments { get; set; } = Array.Empty<NoteAttachmentItemViewModel>();

    public IReadOnlyList<NoteRatingItemViewModel> Ratings { get; set; } = Array.Empty<NoteRatingItemViewModel>();

    public double AverageRating { get; set; }

    public int RatingCount { get; set; }

    public bool CanEdit { get; set; }

    public bool CanDelete { get; set; }

    public bool CanManageShareLinks { get; set; }

    public bool CanRate { get; set; }

    public string? ActiveShareUrl { get; set; }

    public NoteRatingInputViewModel RatingInput { get; set; } = new() { Score = 5 };

    public bool AccessedByShareLink { get; set; }

    public string? AttachmentToken { get; set; }
}
