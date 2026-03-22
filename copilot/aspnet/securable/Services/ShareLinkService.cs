using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services.Interfaces;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Services;

/// <summary>
/// Share link lifecycle: create, revoke, resolve.
/// Token uniqueness enforced by DB unique index (Authenticity).
/// </summary>
public class ShareLinkService : IShareLinkService
{
    private readonly ApplicationDbContext _db;
    private readonly ILogger<ShareLinkService> _logger;

    public ShareLinkService(ApplicationDbContext db, ILogger<ShareLinkService> logger)
    {
        _db = db;
        _logger = logger;
    }

    /// <inheritdoc />
    public async Task<ShareLink> CreateShareLinkAsync(int noteId, string userId)
    {
        ArgumentNullException.ThrowIfNull(userId);

        var note = await _db.Notes.AsNoTracking()
            .FirstOrDefaultAsync(n => n.Id == noteId && n.UserId == userId);

        if (note is null)
        {
            throw new InvalidOperationException("Note not found or access denied.");
        }

        var link = new ShareLink
        {
            NoteId = noteId,
            Token = Guid.NewGuid().ToString("N"),
            CreatedAt = DateTime.UtcNow
        };

        _db.ShareLinks.Add(link);
        await _db.SaveChangesAsync();

        _logger.LogInformation("Share link created for note {NoteId} by user {UserId}", noteId, userId);
        return link;
    }

    /// <inheritdoc />
    public async Task<bool> RevokeShareLinkAsync(int noteId, string userId)
    {
        ArgumentNullException.ThrowIfNull(userId);

        var links = await _db.ShareLinks
            .Where(s => s.NoteId == noteId && !s.IsRevoked)
            .Include(s => s.Note)
            .ToListAsync();

        var ownedLinks = links.Where(s => s.Note?.UserId == userId).ToList();

        if (!ownedLinks.Any()) return false;

        foreach (var link in ownedLinks)
        {
            link.IsRevoked = true;
        }

        await _db.SaveChangesAsync();

        _logger.LogInformation("Revoked {Count} share link(s) for note {NoteId}", ownedLinks.Count, noteId);
        return true;
    }

    /// <inheritdoc />
    public async Task<Note?> GetNoteByShareTokenAsync(string token)
    {
        ArgumentNullException.ThrowIfNull(token);

        // Trust boundary: validate token format before hitting DB
        if (token.Length > 64) return null;

        var now = DateTime.UtcNow;
        var shareLink = await _db.ShareLinks
            .AsNoTracking()
            .Include(s => s.Note)
                .ThenInclude(n => n!.Attachments)
            .FirstOrDefaultAsync(s =>
                s.Token == token &&
                !s.IsRevoked &&
                (s.ExpiresAt == null || s.ExpiresAt > now));

        return shareLink?.Note;
    }
}
