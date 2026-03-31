using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace LooseNotes.Models;

public class ShareLink
{
    [Key]
    public int Id { get; set; }

    [Required]
    [MaxLength(100)]
    public string Token { get; set; } = string.Empty;

    public bool IsActive { get; set; } = true;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public DateTime? LastAccessedAt { get; set; }

    public int AccessCount { get; set; } = 0;

    [Required]
    public int NoteId { get; set; }

    [ForeignKey(nameof(NoteId))]
    public virtual Note? Note { get; set; }
}
