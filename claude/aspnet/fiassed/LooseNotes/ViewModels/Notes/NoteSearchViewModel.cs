using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Notes;

public sealed class NoteSearchViewModel
{
    [MaxLength(200, ErrorMessage = "Search query must not exceed 200 characters.")]
    public string? Query { get; set; }

    public IList<NoteListItemViewModel> Results { get; set; } = [];
    public bool HasSearched { get; set; }
}
