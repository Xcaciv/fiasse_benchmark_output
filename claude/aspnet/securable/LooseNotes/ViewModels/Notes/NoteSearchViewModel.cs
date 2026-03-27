// NoteSearchViewModel.cs — Carries search query and results.
// Integrity: Query is sanitized in the controller before being used in LINQ.
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public sealed class NoteSearchViewModel
{
    [StringLength(200)]
    public string Query { get; set; } = string.Empty;

    public IReadOnlyList<NoteListItemViewModel> Results { get; set; } = Array.Empty<NoteListItemViewModel>();
}
