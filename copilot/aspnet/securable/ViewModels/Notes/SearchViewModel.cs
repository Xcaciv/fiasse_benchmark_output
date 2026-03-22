namespace LooseNotes.ViewModels.Notes;

public sealed class SearchViewModel
{
    public string? Query { get; set; }
    public IReadOnlyList<NoteCardViewModel> Results { get; set; } = Array.Empty<NoteCardViewModel>();
}
