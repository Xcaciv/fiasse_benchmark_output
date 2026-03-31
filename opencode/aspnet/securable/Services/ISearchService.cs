using LooseNotes.Models;

namespace LooseNotes.Services;

public interface ISearchService
{
    Task<IEnumerable<Note>> SearchNotesAsync(string query, string? userId = null);
}
