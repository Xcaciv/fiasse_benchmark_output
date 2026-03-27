using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public class NoteListViewModel
{
    public IEnumerable<Note> Notes { get; set; } = new List<Note>();
    public int CurrentPage { get; set; }
    public int TotalPages { get; set; }
    public string? SearchTerm { get; set; }
}
