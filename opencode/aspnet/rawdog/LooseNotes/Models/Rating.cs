using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace LooseNotes.Models;

public class Rating
{
    public int Id { get; set; }
    
    [Required]
    [Range(1, 5)]
    public int Value { get; set; }
    
    [StringLength(500)]
    public string? Comment { get; set; }
    
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;
    
    [Required]
    public string UserId { get; set; } = string.Empty;
    
    [ForeignKey("UserId")]
    public ApplicationUser User { get; set; } = null!;
    
    public int NoteId { get; set; }
    
    [ForeignKey("NoteId")]
    public Note Note { get; set; } = null!;
}
