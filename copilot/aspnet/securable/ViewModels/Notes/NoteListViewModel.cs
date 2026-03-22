using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public class NoteListViewModel
{
    public IEnumerable<Note> Notes { get; set; } = Enumerable.Empty<Note>();
    public string? SearchQuery { get; set; }
}
