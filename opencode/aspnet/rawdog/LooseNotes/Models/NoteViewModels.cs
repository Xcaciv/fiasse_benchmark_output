using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class NoteViewModel
{
    public int Id { get; set; }
    
    [Required]
    [StringLength(200, MinimumLength = 1)]
    public string Title { get; set; } = string.Empty;
    
    [Required]
    public string Content { get; set; } = string.Empty;
    
    public bool IsPublic { get; set; } = false;
    
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    
    public string? UserName { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public List<Attachment> Attachments { get; set; } = new();
    public List<RatingViewModel> Ratings { get; set; } = new();
}

public class CreateNoteViewModel
{
    [Required]
    [StringLength(200, MinimumLength = 1)]
    [Display(Name = "Title")]
    public string Title { get; set; } = string.Empty;
    
    [Required]
    [Display(Name = "Content")]
    public string Content { get; set; } = string.Empty;
    
    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; } = false;
}

public class EditNoteViewModel
{
    public int Id { get; set; }
    
    [Required]
    [StringLength(200, MinimumLength = 1)]
    [Display(Name = "Title")]
    public string Title { get; set; } = string.Empty;
    
    [Required]
    [Display(Name = "Content")]
    public string Content { get; set; } = string.Empty;
    
    [Display(Name = "Make Public")]
    public bool IsPublic { get; set; } = false;
}

public class RatingViewModel
{
    public int Id { get; set; }
    public int Value { get; set; }
    public string? Comment { get; set; }
    public string UserName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
}

public class CreateRatingViewModel
{
    [Required]
    [Range(1, 5)]
    public int Value { get; set; }
    
    [StringLength(500)]
    public string? Comment { get; set; }
}

public class SearchResultViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Excerpt { get; set; } = string.Empty;
    public string Author { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public bool IsPublic { get; set; }
}

public class ShareLinkViewModel
{
    public int Id { get; set; }
    public string Token { get; set; } = string.Empty;
    public bool IsActive { get; set; }
    public DateTime CreatedAt { get; set; }
    public DateTime? LastAccessedAt { get; set; }
    public int AccessCount { get; set; }
}
