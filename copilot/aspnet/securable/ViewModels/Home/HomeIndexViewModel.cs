using LooseNotes.ViewModels.Notes;

namespace LooseNotes.ViewModels.Home;

public sealed class HomeIndexViewModel
{
    public int PublicNoteCount { get; set; }
    public int UserCount { get; set; }
    public IReadOnlyList<NoteCardViewModel> RecentPublicNotes { get; set; } = Array.Empty<NoteCardViewModel>();
    public IReadOnlyList<NoteCardViewModel> TopRatedNotes { get; set; } = Array.Empty<NoteCardViewModel>();
}
