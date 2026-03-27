namespace LooseNotes.Models;

public class Attachment
{
    public int Id { get; set; }
    public string FileName { get; set; } = string.Empty;
    public string StoredFileName { get; set; } = string.Empty;
    public string ContentType { get; set; } = string.Empty;
    public long FileSize { get; set; }
    public DateTime UploadedAt { get; set; } = DateTime.UtcNow;

    public int NoteId { get; set; }
    public Note? Note { get; set; }
}
