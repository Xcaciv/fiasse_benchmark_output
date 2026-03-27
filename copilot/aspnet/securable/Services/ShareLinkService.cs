using System.Security.Cryptography;
using LooseNotes.Data;
using LooseNotes.Models;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Services;

public class ShareLinkService : IShareLinkService
{
    private readonly ApplicationDbContext _db;
    private readonly ILogger<ShareLinkService> _logger;

    public ShareLinkService(ApplicationDbContext db, ILogger<ShareLinkService> logger)
    {
        _db = db;
        _logger = logger;
    }

    // Generates a cryptographically random base64url token
    private static string GenerateToken()
    {
        var bytes = RandomNumberGenerator.GetBytes(32);
        return Convert.ToBase64String(bytes)
            .Replace('+', '-')
            .Replace('/', '_')
            .TrimEnd('=');
    }

    public async Task<ShareLink> GenerateShareLinkAsync(int noteId, string ownerId, CancellationToken cancellationToken = default)
    {
        // Verify ownership — Derived Integrity Principle: never trust client-supplied ownership
        var note = await _db.Notes
            .FirstOrDefaultAsync(n => n.Id == noteId && n.OwnerId == ownerId, cancellationToken);

        if (note is null)
        {
            throw new UnauthorizedAccessException("Note not found or access denied.");
        }

        // Revoke any existing active links before creating a new one
        var existingLinks = await _db.ShareLinks
            .Where(s => s.NoteId == noteId && s.IsActive)
            .ToListAsync(cancellationToken);

        foreach (var existing in existingLinks)
        {
            existing.IsActive = false;
        }

        var shareLink = new ShareLink
        {
            NoteId = noteId,
            Token = GenerateToken(),
            CreatedAt = DateTime.UtcNow,
            IsActive = true
        };

        _db.ShareLinks.Add(shareLink);
        await _db.SaveChangesAsync(cancellationToken);

        _logger.LogInformation("Share link generated for NoteId={NoteId} by Owner={OwnerId}", noteId, ownerId);
        return shareLink;
    }

    public async Task<bool> RevokeShareLinkAsync(int noteId, string ownerId, CancellationToken cancellationToken = default)
    {
        var note = await _db.Notes
            .FirstOrDefaultAsync(n => n.Id == noteId && n.OwnerId == ownerId, cancellationToken);

        if (note is null)
        {
            return false;
        }

        var activeLinks = await _db.ShareLinks
            .Where(s => s.NoteId == noteId && s.IsActive)
            .ToListAsync(cancellationToken);

        foreach (var link in activeLinks)
        {
            link.IsActive = false;
        }

        await _db.SaveChangesAsync(cancellationToken);
        _logger.LogInformation("Share links revoked for NoteId={NoteId} by Owner={OwnerId}", noteId, ownerId);
        return true;
    }

    public async Task<Note?> GetNoteByTokenAsync(string token, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(token))
        {
            return null;
        }

        var shareLink = await _db.ShareLinks
            .Include(s => s.Note)
                .ThenInclude(n => n!.Owner)
            .Include(s => s.Note)
                .ThenInclude(n => n!.Attachments)
            .Include(s => s.Note)
                .ThenInclude(n => n!.Ratings)
            .FirstOrDefaultAsync(s => s.Token == token && s.IsActive, cancellationToken);

        return shareLink?.Note;
    }
}
