using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public class NoteSearchViewModel
{
    public string? Query { get; set; }
    public IReadOnlyList<Note> Results { get; set; } = Array.Empty<Note>();
}
