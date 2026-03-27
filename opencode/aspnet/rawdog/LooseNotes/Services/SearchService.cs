using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Services;

public interface ISearchService
{
    Task<List<SearchResultViewModel>> SearchNotesAsync(string query, string? userId = null);
    Task<List<Note>> GetTopRatedNotesAsync(int count = 20);
}

public class SearchService : ISearchService
{
    private readonly ApplicationDbContext _context;

    public SearchService(ApplicationDbContext context)
    {
        _context = context;
    }

    public async Task<List<SearchResultViewModel>> SearchNotesAsync(string query, string? userId = null)
    {
        if (string.IsNullOrWhiteSpace(query))
            return new List<SearchResultViewModel>();

        var queryLower = query.ToLowerInvariant();

        var notesQuery = _context.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n => 
                (n.UserId == userId) || 
                (n.IsPublic))
            .Where(n => 
                n.Title.ToLower().Contains(queryLower) || 
                n.Content.ToLower().Contains(queryLower));

        var notes = await notesQuery
            .OrderByDescending(n => n.CreatedAt)
            .Take(50)
            .ToListAsync();

        return notes.Select(n => new SearchResultViewModel
        {
            Id = n.Id,
            Title = n.Title,
            Excerpt = n.Content.Length > 200 ? n.Content.Substring(0, 200) + "..." : n.Content,
            Author = n.User?.UserName ?? "Unknown",
            CreatedAt = n.CreatedAt,
            AverageRating = n.Ratings.Count > 0 ? n.Ratings.Average(r => r.Value) : 0,
            RatingCount = n.Ratings.Count,
            IsPublic = n.IsPublic
        }).ToList();
    }

    public async Task<List<Note>> GetTopRatedNotesAsync(int count = 20)
    {
        return await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n => n.IsPublic && n.Ratings.Count >= 3)
            .OrderByDescending(n => n.Ratings.Average(r => r.Value))
            .Take(count)
            .ToListAsync();
    }
}
