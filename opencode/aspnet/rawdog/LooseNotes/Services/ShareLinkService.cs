using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Services;

public interface IShareLinkService
{
    Task<string> GenerateShareLinkAsync(int noteId);
    Task<bool> RevokeShareLinkAsync(string token);
    Task<ShareLink?> GetShareLinkAsync(string token);
    Task<Note?> GetNoteByShareTokenAsync(string token);
}

public class ShareLinkService : IShareLinkService
{
    private readonly ApplicationDbContext _context;
    private readonly UserManager<ApplicationUser> _userManager;

    public ShareLinkService(ApplicationDbContext context, UserManager<ApplicationUser> userManager)
    {
        _context = context;
        _userManager = userManager;
    }

    public async Task<string> GenerateShareLinkAsync(int noteId)
    {
        var note = await _context.Notes.FindAsync(noteId);
        if (note == null)
            return string.Empty;

        foreach (var existingLink in note.ShareLinks.Where(l => l.IsActive))
        {
            existingLink.IsActive = false;
        }

        var newToken = Guid.NewGuid().ToString("N");
        var shareLink = new ShareLink
        {
            Token = newToken,
            NoteId = noteId,
            IsActive = true,
            CreatedAt = DateTime.UtcNow
        };

        _context.ShareLinks.Add(shareLink);
        await _context.SaveChangesAsync();

        return newToken;
    }

    public async Task<bool> RevokeShareLinkAsync(string token)
    {
        var shareLink = await _context.ShareLinks
            .FirstOrDefaultAsync(s => s.Token == token);

        if (shareLink == null)
            return false;

        shareLink.IsActive = false;
        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<ShareLink?> GetShareLinkAsync(string token)
    {
        return await _context.ShareLinks
            .Include(s => s.Note)
            .ThenInclude(n => n.User)
            .Include(s => s.Note)
            .ThenInclude(n => n.Ratings)
            .Include(s => s.Note)
            .ThenInclude(n => n.Attachments)
            .FirstOrDefaultAsync(s => s.Token == token && s.IsActive);
    }

    public async Task<Note?> GetNoteByShareTokenAsync(string token)
    {
        var shareLink = await _context.ShareLinks
            .Include(s => s.Note)
            .ThenInclude(n => n.User)
            .Include(s => s.Note)
            .ThenInclude(n => n.Ratings)
            .ThenInclude(r => r.User)
            .Include(s => s.Note)
            .ThenInclude(n => n.Attachments)
            .FirstOrDefaultAsync(s => s.Token == token && s.IsActive);

        if (shareLink == null)
            return null;

        shareLink.AccessCount++;
        shareLink.LastAccessedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        return shareLink.Note;
    }
}
