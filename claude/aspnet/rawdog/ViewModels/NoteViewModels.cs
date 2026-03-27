using System.ComponentModel.DataAnnotations;
using LooseNotes.Models;

namespace LooseNotes.ViewModels;

public class CreateNoteViewModel
{
    [Required]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    public bool IsPublic { get; set; }

    public List<IFormFile> Attachments { get; set; } = new();
}

public class EditNoteViewModel
{
    public int Id { get; set; }

    [Required]
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
    public Rating? CurrentUserRating { get; set; }
    public bool CanEdit { get; set; }
    public string? ShareToken { get; set; }
}

public class RatingViewModel
{
    [Required, Range(1, 5)]
    public int Stars { get; set; }

    public string? Comment { get; set; }

    public int NoteId { get; set; }
}

public class SearchViewModel
{
    public string Query { get; set; } = string.Empty;
    public List<Note> Results { get; set; } = new();
}

public class TopRatedViewModel
{
    public List<(Note Note, double Avg, int Count)> Items { get; set; } = new();
}
