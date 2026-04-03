using System.ComponentModel.DataAnnotations;
using LooseNotes.Models;

namespace LooseNotes.ViewModels;

public class NoteCreateViewModel
{
    [Required]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    public bool IsPublic { get; set; } = false;
    public string? Tags { get; set; }
}

public class NoteEditViewModel
{
    public int Id { get; set; }

    [Required]
    public string Title { get; set; } = string.Empty;

    [Required]
    public string Content { get; set; } = string.Empty;

    public bool IsPublic { get; set; }
    public string? Tags { get; set; }
}

public class NoteDetailsViewModel
{
    public Note Note { get; set; } = null!;
    public List<Rating> Ratings { get; set; } = new();
    public RatingSubmitViewModel NewRating { get; set; } = new();
}

public class RatingSubmitViewModel
{
    public int NoteId { get; set; }
    public int Score { get; set; } = 3;
    public string Comment { get; set; } = string.Empty;
}

public class NoteSearchViewModel
{
    public string Keyword { get; set; } = string.Empty;
    public List<Note> Results { get; set; } = new();
}

public class AttachFileViewModel
{
    public int NoteId { get; set; }
    public IFormFile? File { get; set; }
}

public class ImportViewModel
{
    public IFormFile? ZipFile { get; set; }
}
