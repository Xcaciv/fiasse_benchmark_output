using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class Attachment
{
    public int Id { get; set; }
    
    [Required]
    public string FileName { get; set; } = string.Empty;
    
    [Required]
    public string StoredFileName { get; set; } = string.Empty;
    
    [Required]
    public string ContentType { get; set; } = string.Empty;
    
    public long FileSize { get; set; }
    
    public DateTime UploadedAt { get; set; } = DateTime.UtcNow;
    
    public int NoteId { get; set; }
    public Note Note { get; set; } = null!;
}
