using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public class NoteSearchResultViewModel
{
    public string Query { get; set; } = string.Empty;
    public IEnumerable<Note> Results { get; set; } = Enumerable.Empty<Note>();
    public int TotalCount { get; set; }
}
