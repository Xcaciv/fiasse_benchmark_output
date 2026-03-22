using System.ComponentModel.DataAnnotations;
using LooseNotes.Models;

namespace LooseNotes.ViewModels;

public class CreateNoteViewModel
{
    [Required]
    [StringLength(200)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    public bool IsPublic { get; set; } = false;

    public List<IFormFile> Attachments { get; set; } = new();
}

public class EditNoteViewModel
{
    public int Id { get; set; }

    [Required]
    [StringLength(200)]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    public bool IsPublic { get; set; }

    public List<IFormFile> NewAttachments { get; set; } = new();

    public List<Attachment> ExistingAttachments { get; set; } = new();
}

public class NoteDetailsViewModel
{
    public Note Note { get; set; } = null!;
    public Rating? UserRating { get; set; }
    public string? ShareToken { get; set; }
    public bool IsOwner { get; set; }
}

public class RateNoteViewModel
{
    public int NoteId { get; set; }

    [Range(1, 5)]
    public int Value { get; set; }

    [StringLength(500)]
    public string? Comment { get; set; }
}

public class SearchViewModel
{
    public string? Query { get; set; }
    public List<NoteSearchResultViewModel> Results { get; set; } = new();
}

public class NoteSearchResultViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Excerpt { get; set; } = string.Empty;
    public string AuthorName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public bool IsPublic { get; set; }
}

public class TopRatedNoteViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string AuthorName { get; set; } = string.Empty;
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public string Preview { get; set; } = string.Empty;
}
