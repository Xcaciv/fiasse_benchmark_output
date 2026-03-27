namespace LooseNotes.ViewModels.Notes;

public class SearchViewModel
{
    public string? Query { get; set; }
    public IList<NoteListItemViewModel> Results { get; set; } = new List<NoteListItemViewModel>();
    public bool HasSearched { get; set; }
}
