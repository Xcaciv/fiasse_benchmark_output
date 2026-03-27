namespace LooseNotes.Models;

public class Rating
{
    public int Id { get; set; }
    public int NoteId { get; set; }
    public string RaterId { get; set; } = string.Empty;
    public int Value { get; set; }
    public string Comment { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    public Note? Note { get; set; }
    public ApplicationUser? Rater { get; set; }
}
