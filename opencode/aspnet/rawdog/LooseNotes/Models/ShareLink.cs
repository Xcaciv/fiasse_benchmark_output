using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class ShareLink
{
    public int Id { get; set; }
    
    [Required]
    public string Token { get; set; } = string.Empty;
    
    public bool IsActive { get; set; } = true;
    
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    
    public DateTime? LastAccessedAt { get; set; }
    
    public int AccessCount { get; set; } = 0;
    
    public int NoteId { get; set; }
    public Note Note { get; set; } = null!;
}
