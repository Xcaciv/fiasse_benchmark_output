using LooseNotes.Data;
using LooseNotes.Models;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Services;

public class ShareLinkService : IShareLinkService
{
    private readonly ApplicationDbContext _context;
    private readonly ILogger<ShareLinkService> _logger;

    public ShareLinkService(ApplicationDbContext context, ILogger<ShareLinkService> logger)
    {
        _context = context;
        _logger = logger;
    }

    public async Task<ShareLink> CreateShareLinkAsync(
        int noteId, string createdById, CancellationToken ct = default)
    {
        var link = new ShareLink
        {
            NoteId = noteId,
            Token = ShareLink.GenerateToken(),
            CreatedById = createdById,
            CreatedAt = DateTimeOffset.UtcNow
        };

        _context.ShareLinks.Add(link);
        await _context.SaveChangesAsync(ct);

        _logger.LogInformation(
            "Share link {LinkId} created for note {NoteId} by user {UserId}",
            link.Id, noteId, createdById);
        return link;
    }

    public async Task<bool> RevokeShareLinkAsync(
        int shareLinkId, string requestingUserId, CancellationToken ct = default)
    {
        // Ownership check enforced here (Authenticity principle)
        var link = await _context.ShareLinks
            .Include(s => s.Note)
            .FirstOrDefaultAsync(s => s.Id == shareLinkId, ct);

        if (link is null) return false;
        if (link.Note.OwnerId != requestingUserId) return false;

        _context.ShareLinks.Remove(link);
        await _context.SaveChangesAsync(ct);

        _logger.LogInformation("Share link {LinkId} revoked by user {UserId}", shareLinkId, requestingUserId);
        return true;
    }

    public async Task<Note?> GetNoteByTokenAsync(string token, CancellationToken ct = default)
    {
        var shareLink = await _context.ShareLinks
            .AsNoTracking()
            .Include(s => s.Note).ThenInclude(n => n.Owner)
            .Include(s => s.Note).ThenInclude(n => n.Attachments)
            .Include(s => s.Note).ThenInclude(n => n.Ratings).ThenInclude(r => r.Rater)
            .FirstOrDefaultAsync(s => s.Token == token, ct);

        return shareLink?.Note;
    }
}
