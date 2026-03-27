using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.ViewModels.Notes;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Services;

public class NoteService : INoteService
{
    private readonly ApplicationDbContext _db;
    private readonly IFileStorageService _fileStorage;
    private readonly ILogger<NoteService> _logger;

    public NoteService(ApplicationDbContext db, IFileStorageService fileStorage, ILogger<NoteService> logger)
    {
        _db = db;
        _fileStorage = fileStorage;
        _logger = logger;
    }

    public async Task<List<NoteListItemViewModel>> GetUserNotesAsync(string userId, CancellationToken cancellationToken = default)
    {
        return await _db.Notes
            .Where(n => n.OwnerId == userId)
            .OrderByDescending(n => n.UpdatedAt)
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > 200 ? n.Content.Substring(0, 200) + "..." : n.Content,
                AuthorName = n.Owner != null ? n.Owner.DisplayName : "Unknown",
                CreatedAt = n.CreatedAt,
                IsPublic = n.IsPublic,
                AverageRating = n.Ratings.Any() ? n.Ratings.Average(r => r.Value) : 0,
                RatingCount = n.Ratings.Count()
            })
            .ToListAsync(cancellationToken);
    }

    public async Task<NoteDetailViewModel?> GetNoteDetailAsync(int noteId, string? currentUserId, CancellationToken cancellationToken = default)
    {
        var note = await _db.Notes
            .Include(n => n.Owner)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.Rater)
            .Include(n => n.ShareLinks)
            .FirstOrDefaultAsync(n => n.Id == noteId, cancellationToken);

        if (note is null) return null;

        // Authorization: only owner can see private notes
        bool isOwner = currentUserId != null && note.OwnerId == currentUserId;
        if (!note.IsPublic && !isOwner) return null;

        var activeShareLink = note.ShareLinks.FirstOrDefault(s => s.IsActive);

        return new NoteDetailViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            AuthorName = note.Owner?.DisplayName ?? "Unknown",
            OwnerId = note.OwnerId,
            IsPublic = note.IsPublic,
            CreatedAt = note.CreatedAt,
            UpdatedAt = note.UpdatedAt,
            Attachments = note.Attachments.ToList(),
            Ratings = note.Ratings.ToList(),
            AverageRating = note.Ratings.Any() ? note.Ratings.Average(r => r.Value) : 0,
            RatingCount = note.Ratings.Count,
            HasActiveShareLink = activeShareLink != null,
            ShareToken = isOwner ? activeShareLink?.Token : null,
            IsOwner = isOwner,
            CurrentUserHasRated = currentUserId != null && note.Ratings.Any(r => r.RaterId == currentUserId)
        };
    }

    public async Task<Note?> GetNoteForEditAsync(int noteId, string ownerId, CancellationToken cancellationToken = default)
    {
        return await _db.Notes
            .FirstOrDefaultAsync(n => n.Id == noteId && n.OwnerId == ownerId, cancellationToken);
    }

    public async Task<Note> CreateNoteAsync(string ownerId, NoteViewModel model, CancellationToken cancellationToken = default)
    {
        var note = new Note
        {
            Title = model.Title,
            Content = model.Content,
            IsPublic = model.IsPublic,
            OwnerId = ownerId,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _db.Notes.Add(note);
        await _db.SaveChangesAsync(cancellationToken);
        _logger.LogInformation("Note created: NoteId={NoteId} OwnerId={OwnerId}", note.Id, ownerId);
        return note;
    }

    public async Task<bool> UpdateNoteAsync(int noteId, string ownerId, NoteViewModel model, CancellationToken cancellationToken = default)
    {
        var note = await _db.Notes
            .FirstOrDefaultAsync(n => n.Id == noteId && n.OwnerId == ownerId, cancellationToken);

        if (note is null) return false;

        note.Title = model.Title;
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync(cancellationToken);
        _logger.LogInformation("Note updated: NoteId={NoteId} OwnerId={OwnerId}", noteId, ownerId);
        return true;
    }

    public async Task<bool> DeleteNoteAsync(int noteId, string ownerId, CancellationToken cancellationToken = default)
    {
        var note = await _db.Notes
            .Include(n => n.Attachments)
            .FirstOrDefaultAsync(n => n.Id == noteId && n.OwnerId == ownerId, cancellationToken);

        if (note is null) return false;

        // Cascade delete attachments from file storage
        foreach (var attachment in note.Attachments)
        {
            try
            {
                await _fileStorage.DeleteFileAsync(attachment.StoredFileName, cancellationToken);
            }
            catch (IOException ex)
            {
                _logger.LogWarning(ex, "Could not delete attachment file: {StoredFileName}", attachment.StoredFileName);
            }
        }

        _db.Notes.Remove(note);
        await _db.SaveChangesAsync(cancellationToken);
        _logger.LogInformation("Note deleted: NoteId={NoteId} OwnerId={OwnerId}", noteId, ownerId);
        return true;
    }

    public async Task<List<NoteListItemViewModel>> SearchNotesAsync(string query, string? currentUserId, CancellationToken cancellationToken = default)
    {
        if (string.IsNullOrWhiteSpace(query))
        {
            return new List<NoteListItemViewModel>();
        }

        var lowerQuery = query.ToLowerInvariant();

        return await _db.Notes
            .Where(n => (n.OwnerId == currentUserId || n.IsPublic) &&
                        (n.Title.ToLower().Contains(lowerQuery) || n.Content.ToLower().Contains(lowerQuery)))
            .OrderByDescending(n => n.UpdatedAt)
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > 200 ? n.Content.Substring(0, 200) + "..." : n.Content,
                AuthorName = n.Owner != null ? n.Owner.DisplayName : "Unknown",
                CreatedAt = n.CreatedAt,
                IsPublic = n.IsPublic,
                AverageRating = n.Ratings.Any() ? n.Ratings.Average(r => r.Value) : 0,
                RatingCount = n.Ratings.Count()
            })
            .ToListAsync(cancellationToken);
    }

    public async Task<List<NoteListItemViewModel>> GetTopRatedNotesAsync(int minRatings = 3, CancellationToken cancellationToken = default)
    {
        return await _db.Notes
            .Where(n => n.IsPublic && n.Ratings.Count() >= minRatings)
            .OrderByDescending(n => n.Ratings.Average(r => r.Value))
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > 200 ? n.Content.Substring(0, 200) + "..." : n.Content,
                AuthorName = n.Owner != null ? n.Owner.DisplayName : "Unknown",
                CreatedAt = n.CreatedAt,
                IsPublic = n.IsPublic,
                AverageRating = n.Ratings.Average(r => r.Value),
                RatingCount = n.Ratings.Count()
            })
            .ToListAsync(cancellationToken);
    }

    public async Task<bool> AddAttachmentAsync(int noteId, string ownerId, IFormFile file, CancellationToken cancellationToken = default)
    {
        var note = await _db.Notes
            .FirstOrDefaultAsync(n => n.Id == noteId && n.OwnerId == ownerId, cancellationToken);

        if (note is null) return false;

        if (!_fileStorage.IsAllowedExtension(file.FileName) || !_fileStorage.IsWithinSizeLimit(file.Length))
        {
            return false;
        }

        var (storedFileName, contentType) = await _fileStorage.SaveFileAsync(file, cancellationToken);

        var attachment = new Attachment
        {
            NoteId = noteId,
            OriginalFileName = Path.GetFileName(file.FileName),
            StoredFileName = storedFileName,
            ContentType = contentType,
            FileSize = file.Length,
            UploadedAt = DateTime.UtcNow
        };

        _db.Attachments.Add(attachment);
        await _db.SaveChangesAsync(cancellationToken);
        return true;
    }

    public async Task<bool> DeleteAttachmentAsync(int attachmentId, string ownerId, CancellationToken cancellationToken = default)
    {
        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == attachmentId && a.Note != null && a.Note.OwnerId == ownerId, cancellationToken);

        if (attachment is null) return false;

        try
        {
            await _fileStorage.DeleteFileAsync(attachment.StoredFileName, cancellationToken);
        }
        catch (IOException ex)
        {
            _logger.LogWarning(ex, "File deletion failed for attachment: {AttachmentId}", attachmentId);
        }

        _db.Attachments.Remove(attachment);
        await _db.SaveChangesAsync(cancellationToken);
        return true;
    }

    public async Task<Attachment?> GetAttachmentAsync(int attachmentId, string? currentUserId, CancellationToken cancellationToken = default)
    {
        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == attachmentId, cancellationToken);

        if (attachment?.Note is null) return null;

        // Authorization: owner or public note
        bool isOwner = currentUserId != null && attachment.Note.OwnerId == currentUserId;
        if (!attachment.Note.IsPublic && !isOwner) return null;

        return attachment;
    }

    public async Task<bool> AddOrUpdateRatingAsync(string raterId, RatingViewModel model, CancellationToken cancellationToken = default)
    {
        // Cannot rate own note — Derived Integrity Principle
        var note = await _db.Notes
            .FirstOrDefaultAsync(n => n.Id == model.NoteId && n.IsPublic, cancellationToken);

        if (note is null || note.OwnerId == raterId) return false;

        var existing = await _db.Ratings
            .FirstOrDefaultAsync(r => r.NoteId == model.NoteId && r.RaterId == raterId, cancellationToken);

        if (existing != null)
        {
            existing.Value = model.Value;
            existing.Comment = model.Comment ?? string.Empty;
            existing.UpdatedAt = DateTime.UtcNow;
        }
        else
        {
            _db.Ratings.Add(new Rating
            {
                NoteId = model.NoteId,
                RaterId = raterId,
                Value = model.Value,
                Comment = model.Comment ?? string.Empty,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
        }

        await _db.SaveChangesAsync(cancellationToken);
        return true;
    }
}
