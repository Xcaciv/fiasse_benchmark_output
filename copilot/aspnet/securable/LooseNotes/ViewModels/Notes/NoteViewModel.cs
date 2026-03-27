namespace LooseNotes.ViewModels.Notes;

public class NoteViewModel
{
    public int Id { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public bool IsPublic { get; set; }
    public DateTimeOffset CreatedAt { get; set; }
    public DateTimeOffset UpdatedAt { get; set; }
    public string OwnerId { get; set; } = string.Empty;
    public string OwnerDisplayName { get; set; } = string.Empty;
}
