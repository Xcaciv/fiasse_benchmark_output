using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class ShareLink
{
    public int Id { get; set; }

    [Required]
    public string Token { get; set; } = string.Empty;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public int NoteId { get; set; }

    public Note? Note { get; set; }
}
