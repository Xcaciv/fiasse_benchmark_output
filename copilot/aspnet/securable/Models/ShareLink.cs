namespace LooseNotes.Models;

public class ShareLink
{
    public int Id { get; set; }
    public int NoteId { get; set; }
    public string Token { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public bool IsActive { get; set; } = true;

    public Note? Note { get; set; }
}
