using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using System.Security.Cryptography;
using System.Text;

namespace LooseNotes.Services;

public class NoteService : INoteService
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<NoteService> _logger;

    public NoteService(ApplicationDbContext context, ILogger<NoteService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<Note> CreateNoteAsync(Note note)
    {
        note.CreatedAt = DateTime.UtcNow;
        _context.Notes.Add(note);
        await _context.SaveChangesAsync();
        
        _logger.LogInformation("Note created: {NoteId} by user {UserId}", note.Id, note.UserId);
        
        return note;
    }

    public async Task<Note?> GetNoteByIdAsync(int id)
    {
        return await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings)
            .FirstOrDefaultAsync(n => n.Id == id);
    }

    public async Task<Note?> GetNoteByIdForUserAsync(int id, string userId)
    {
        return await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings)
            .FirstOrDefaultAsync(n => n.Id == id && (n.UserId == userId || n.IsPublic));
    }

    public async Task<Note?> GetPublicNoteByIdAsync(int id)
    {
        return await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings)
            .FirstOrDefaultAsync(n => n.Id == id && n.IsPublic);
    }

    public async Task<Note?> GetSharedNoteByTokenAsync(string token)
    {
        var shareLink = await _context.ShareLinks
            .Include(s => s.Note)
                .ThenInclude(n => n!.User)
            .Include(s => s.Note)
                .ThenInclude(n => n!.Attachments)
            .Include(s => s.Note)
                .ThenInclude(n => n!.Ratings)
            .FirstOrDefaultAsync(s => s.Token == token && s.IsActive);

        if (shareLink != null)
        {
            shareLink.LastAccessedAt = DateTime.UtcNow;
            shareLink.AccessCount++;
            await _context.SaveChangesAsync();
        }

        return shareLink?.Note;
    }

    public async Task<IEnumerable<Note>> GetUserNotesAsync(string userId)
    {
        return await _context.Notes
            .Where(n => n.UserId == userId)
            .OrderByDescending(n => n.ModifiedAt ?? n.CreatedAt)
            .ToListAsync();
    }

    public async Task<IEnumerable<Note>> GetPublicNotesAsync()
    {
        return await _context.Notes
            .Where(n => n.IsPublic)
            .OrderByDescending(n => n.ModifiedAt ?? n.CreatedAt)
            .ToListAsync();
    }

    public async Task<IEnumerable<Note>> GetTopRatedNotesAsync(int minimumRatings = 3)
    {
        var notes = await _context.Notes
            .Where(n => n.IsPublic)
            .Include(n => n.Ratings)
            .Include(n => n.User)
            .ToListAsync();
        
        return notes
            .Where(n => n.RatingCount >= minimumRatings)
            .OrderByDescending(n => n.AverageRating)
            .Take(50)
            .ToList();
    }

    public async Task<Note> UpdateNoteAsync(Note note)
    {
        var existingNote = await _context.Notes.FindAsync(note.Id);
        if (existingNote == null)
        {
            throw new InvalidOperationException("Note not found");
        }

        existingNote.Title = note.Title;
        existingNote.Content = note.Content;
        existingNote.IsPublic = note.IsPublic;
        existingNote.ModifiedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        
        _logger.LogInformation("Note updated: {NoteId}", note.Id);
        
        return existingNote;
    }

    public async Task<bool> DeleteNoteAsync(int id, string userId, bool isAdmin)
    {
        var note = await _context.Notes
            .Include(n => n.Attachments)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note == null)
        {
            return false;
        }

        if (!isAdmin && note.UserId != userId)
        {
            return false;
        }

        _context.Notes.Remove(note);
        await _context.SaveChangesAsync();
        
        _logger.LogInformation("Note deleted: {NoteId} by user {UserId} (Admin: {IsAdmin})", 
            id, userId, isAdmin);
        
        return true;
    }

    public async Task<ShareLink> CreateShareLinkAsync(int noteId)
    {
        var existingLink = await _context.ShareLinks
            .FirstOrDefaultAsync(s => s.NoteId == noteId && s.IsActive);

        if (existingLink != null)
        {
            return existingLink;
        }

        var shareLink = new ShareLink
        {
            NoteId = noteId,
            Token = GenerateSecureToken(),
            IsActive = true,
            CreatedAt = DateTime.UtcNow
        };

        _context.ShareLinks.Add(shareLink);
        await _context.SaveChangesAsync();
        
        _logger.LogInformation("Share link created for note: {NoteId}", noteId);
        
        return shareLink;
    }

    public async Task<ShareLink?> GetActiveShareLinkAsync(int noteId)
    {
        return await _context.ShareLinks
            .FirstOrDefaultAsync(s => s.NoteId == noteId && s.IsActive);
    }

    public async Task<bool> RevokeShareLinkAsync(int noteId, string userId)
    {
        var note = await _context.Notes.FindAsync(noteId);
        if (note == null || note.UserId != userId)
        {
            return false;
        }

        var shareLink = await _context.ShareLinks
            .FirstOrDefaultAsync(s => s.NoteId == noteId && s.IsActive);

        if (shareLink != null)
        {
            shareLink.IsActive = false;
            await _context.SaveChangesAsync();
            
            _logger.LogInformation("Share link revoked for note: {NoteId}", noteId);
        }

        return true;
    }

    public async Task<IEnumerable<Rating>> GetNoteRatingsAsync(int noteId)
    {
        return await _context.Ratings
            .Include(r => r.User)
            .Where(r => r.NoteId == noteId)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();
    }

    public async Task<Rating> AddOrUpdateRatingAsync(int noteId, string userId, int value, string? comment)
    {
        var existingRating = await _context.Ratings
            .FirstOrDefaultAsync(r => r.NoteId == noteId && r.UserId == userId);

        if (existingRating != null)
        {
            existingRating.Value = value;
            existingRating.Comment = comment;
            existingRating.ModifiedAt = DateTime.UtcNow;
            await _context.SaveChangesAsync();
            
            _logger.LogInformation("Rating updated for note: {NoteId} by user {UserId}", noteId, userId);
            
            return existingRating;
        }

        var rating = new Rating
        {
            NoteId = noteId,
            UserId = userId,
            Value = value,
            Comment = comment,
            CreatedAt = DateTime.UtcNow
        };

        _context.Ratings.Add(rating);
        await _context.SaveChangesAsync();
        
        _logger.LogInformation("Rating added for note: {NoteId} by user {UserId}", noteId, userId);
        
        return rating;
    }

    public async Task<bool> CanUserAccessNoteAsync(int noteId, string userId, bool isAdmin)
    {
        var note = await _context.Notes.FindAsync(noteId);
        if (note == null) return false;
        
        return isAdmin || note.UserId == userId || note.IsPublic;
    }

    private static string GenerateSecureToken()
    {
        var bytes = new byte[32];
        using var rng = RandomNumberGenerator.Create();
        rng.GetBytes(bytes);
        return Convert.ToBase64String(bytes);
    }
}
