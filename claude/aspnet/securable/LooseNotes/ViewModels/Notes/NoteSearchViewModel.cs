using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public class NoteSearchViewModel
{
    [MaxLength(200)]
    public string? Query { get; set; }

    public IReadOnlyList<NoteListItemViewModel> Results { get; set; } = Array.Empty<NoteListItemViewModel>();

    public bool HasSearched { get; set; }
}
