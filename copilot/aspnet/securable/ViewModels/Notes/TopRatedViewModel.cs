namespace LooseNotes.ViewModels.Notes;

public sealed class TopRatedViewModel
{
    public IReadOnlyList<NoteCardViewModel> Notes { get; set; } = Array.Empty<NoteCardViewModel>();
}
