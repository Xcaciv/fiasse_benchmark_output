using System.ComponentModel.DataAnnotations;
using LooseNotes.Models;

namespace LooseNotes.ViewModels;

public class CreateNoteViewModel
{
    [Required]
    [StringLength(300, MinimumLength = 1)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; } = false;

    [Display(Name = "Attachments")]
    public List<IFormFile> Files { get; set; } = new();
}

public class EditNoteViewModel
{
    public int Id { get; set; }

    [Required]
    [StringLength(300, MinimumLength = 1)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; } = false;

    [Display(Name = "Add Attachments")]
    public List<IFormFile> Files { get; set; } = new();

    public List<Attachment> ExistingAttachments { get; set; } = new();
}

public class NoteDetailViewModel
{
    public Note Note { get; set; } = null!;
    public double AverageRating { get; set; }
    public Rating? CurrentUserRating { get; set; }
    public bool CanEdit { get; set; }
    public bool CanDelete { get; set; }
    public List<ShareLink> ActiveShareLinks { get; set; } = new();
}

public class NoteListItemViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Excerpt { get; set; } = string.Empty;
    public string AuthorName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public bool IsPublic { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public int AttachmentCount { get; set; }
}

public class SearchViewModel
{
    public string Query { get; set; } = string.Empty;
    public List<NoteListItemViewModel> Results { get; set; } = new();
}

public class TopRatedViewModel
{
    public List<NoteListItemViewModel> Notes { get; set; } = new();
}

public class CreateRatingViewModel
{
    [Required]
    public int NoteId { get; set; }

    [Required]
    [Range(1, 5, ErrorMessage = "Rating must be between 1 and 5.")]
    public int Stars { get; set; }

    [StringLength(1000)]
    public string? Comment { get; set; }
}

public class EditRatingViewModel
{
    [Required]
    public int Id { get; set; }

    [Required]
    [Range(1, 5, ErrorMessage = "Rating must be between 1 and 5.")]
    public int Stars { get; set; }

    [StringLength(1000)]
    public string? Comment { get; set; }
}
