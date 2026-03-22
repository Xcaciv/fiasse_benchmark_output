using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services.Interfaces;
using LooseNotes.ViewModels.Notes;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Services;

/// <summary>
/// Implements note CRUD with ownership checks, search, and top-rated queries.
/// All mutations verify ownership before proceeding (Integrity + Authenticity).
/// </summary>
public class NoteService : INoteService
{
    private readonly ApplicationDbContext _db;
    private readonly ILogger<NoteService> _logger;

    public NoteService(ApplicationDbContext db, ILogger<NoteService> logger)
    {
        _db = db;
        _logger = logger;
    }

    /// <inheritdoc />
    public async Task<Note?> GetNoteAsync(int id, string? requestingUserId)
    {
        var note = await _db.Notes
            .AsNoTracking()
            .Include(n => n.User)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.User)
            .Include(n => n.ShareLinks)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note is null) return null;
        if (note.UserId == requestingUserId) return note;
        if (note.IsPublic) return note;

        return null; // deny access — private note not owned by requester
    }

    /// <inheritdoc />
    public async Task<IEnumerable<Note>> GetUserNotesAsync(string userId)
    {
        ArgumentNullException.ThrowIfNull(userId);

        return await _db.Notes
            .AsNoTracking()
            .Where(n => n.UserId == userId)
            .Include(n => n.Ratings)
            .OrderByDescending(n => n.UpdatedAt)
            .ToListAsync();
    }

    /// <inheritdoc />
    public async Task<IEnumerable<Note>> SearchNotesAsync(string query, string? requestingUserId)
    {
        if (string.IsNullOrWhiteSpace(query)) return Enumerable.Empty<Note>();

        var lower = query.ToLower();

        return await _db.Notes
            .AsNoTracking()
            .Include(n => n.User)
            .Where(n =>
                (n.UserId == requestingUserId ||  // owner sees all their notes
                 n.IsPublic) &&                  // others see only public
                (n.Title.ToLower().Contains(lower) ||
                 n.Content.ToLower().Contains(lower)))
            .OrderByDescending(n => n.UpdatedAt)
            .Take(100) // cap results (Availability)
            .ToListAsync();
    }

    /// <inheritdoc />
    public async Task<Note> CreateNoteAsync(NoteCreateViewModel model, string userId)
    {
        ArgumentNullException.ThrowIfNull(model);
        ArgumentNullException.ThrowIfNull(userId);

        var note = new Note
        {
            Title = model.Title,
            Content = model.Content,
            IsPublic = model.IsPublic,
            UserId = userId,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _db.Notes.Add(note);
        await _db.SaveChangesAsync();

        _logger.LogInformation("Note {NoteId} created by user {UserId}", note.Id, userId);
        return note;
    }

    /// <inheritdoc />
    public async Task<bool> UpdateNoteAsync(int id, NoteEditViewModel model, string userId)
    {
        ArgumentNullException.ThrowIfNull(model);
        ArgumentNullException.ThrowIfNull(userId);

        var note = await _db.Notes.FirstOrDefaultAsync(n => n.Id == id && n.UserId == userId);
        if (note is null) return false;

        note.Title = model.Title;
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();
        return true;
    }

    /// <inheritdoc />
    public async Task<bool> DeleteNoteAsync(int id, string userId, bool isAdmin)
    {
        ArgumentNullException.ThrowIfNull(userId);

        var note = await _db.Notes
            .Include(n => n.Attachments)
            .FirstOrDefaultAsync(n => n.Id == id && (n.UserId == userId || isAdmin));

        if (note is null) return false;

        _db.Notes.Remove(note); // EF cascade handles related entities
        await _db.SaveChangesAsync();

        _logger.LogInformation("Note {NoteId} deleted by user {UserId} (isAdmin={IsAdmin})", id, userId, isAdmin);
        return true;
    }

    /// <inheritdoc />
    public async Task<IEnumerable<Note>> GetTopRatedNotesAsync(int minRatings = 3)
    {
        return await _db.Notes
            .AsNoTracking()
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n => n.IsPublic && n.Ratings.Count >= minRatings)
            .OrderByDescending(n => n.Ratings.Average(r => r.Stars))
            .Take(20) // cap result set (Availability)
            .ToListAsync();
    }
}
