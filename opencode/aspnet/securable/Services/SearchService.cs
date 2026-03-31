using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Services;

public class SearchService : ISearchService
{
    private readonly ApplicationDbContext _context;

    public SearchService(ApplicationDbContext context)
    {
        _context = context;
    }

    public async Task<IEnumerable<Note>> SearchNotesAsync(string query, string? userId = null)
    {
        if (string.IsNullOrWhiteSpace(query))
        {
            return Enumerable.Empty<Note>();
        }

        var searchTerm = query.ToLower();

        IQueryable<Note> notesQuery = _context.Notes
            .Include(n => n.User)
            .Where(n => n.IsPublic || n.UserId == userId);

        var results = await notesQuery
            .ToListAsync();

        return results
            .Where(n => 
                (n.Title != null && n.Title.ToLower().Contains(searchTerm)) ||
                (n.Content != null && n.Content.ToLower().Contains(searchTerm)))
            .OrderByDescending(n => n.ModifiedAt ?? n.CreatedAt)
            .ToList();
    }
}
