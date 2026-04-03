namespace LooseNotes.Models;

public class Rating
{
    public int Id { get; set; }
    public int Score { get; set; }
    public string Comment { get; set; } = string.Empty;
    public string SubmitterEmail { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public int NoteId { get; set; }
    public Note? Note { get; set; }
}
