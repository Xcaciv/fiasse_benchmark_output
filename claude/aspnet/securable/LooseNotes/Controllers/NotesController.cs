// NotesController.cs — CRUD for notes plus sharing and search.
// Trust boundary: ownership verified on every mutating operation (IDOR prevention).
// Integrity: all queries use EF Core parameterized LINQ.
// Accountability: delete, visibility changes, and sharing are audited.
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Notes;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

[Authorize]
public sealed class NotesController : Controller
{
    private const int ExcerptLength = 200;

    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IFileStorageService _fileStorage;
    private readonly IShareTokenService _shareTokenService;
    private readonly IAuditService _auditService;
    private readonly ILogger<NotesController> _logger;

    public NotesController(
        ApplicationDbContext db,
        UserManager<ApplicationUser> userManager,
        IFileStorageService fileStorage,
        IShareTokenService shareTokenService,
        IAuditService auditService,
        ILogger<NotesController> logger)
    {
        _db = db;
        _userManager = userManager;
        _fileStorage = fileStorage;
        _shareTokenService = shareTokenService;
        _auditService = auditService;
        _logger = logger;
    }

    // ── GET /Notes ────────────────────────────────────────────────────────────
    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var userId = _userManager.GetUserId(User)!;

        var notes = await _db.Notes
            .Where(n => n.UserId == userId)
            .OrderByDescending(n => n.UpdatedAt)
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > ExcerptLength
                    ? n.Content.Substring(0, ExcerptLength) + "…"
                    : n.Content,
                Visibility = n.Visibility,
                AuthorUserName = n.User!.UserName!,
                CreatedAt = n.CreatedAt,
                AverageRating = n.Ratings.Any() ? n.Ratings.Average(r => r.Value) : 0,
                RatingCount = n.Ratings.Count
            })
            .ToListAsync();

        return View(notes);
    }

    // ── GET /Notes/Details/5 ──────────────────────────────────────────────────
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Details(int id)
    {
        var userId = _userManager.GetUserId(User);

        var note = await _db.Notes
            .Include(n => n.User)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.User)
            .Include(n => n.ShareLinks.Where(s => s.IsActive))
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note is null) return NotFound();

        // Authorization: private notes only visible to owner
        if (note.Visibility == NoteVisibility.Private && note.UserId != userId)
            return note.UserId == userId ? View(MapToDetails(note, userId)) : Forbid();

        return View(MapToDetails(note, userId));
    }

    // ── GET /Notes/Create ─────────────────────────────────────────────────────
    [HttpGet]
    public IActionResult Create() => View(new CreateNoteViewModel());

    // ── POST /Notes/Create ────────────────────────────────────────────────────
    [HttpPost]
    public async Task<IActionResult> Create(CreateNoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var userId = _userManager.GetUserId(User)!;

        var note = new Note
        {
            Title = model.Title.Trim(),
            Content = model.Content,
            UserId = userId,
            Visibility = model.Visibility
        };

        _db.Notes.Add(note);
        await _db.SaveChangesAsync();

        if (model.Attachment is not null)
            await SaveAttachmentAsync(note.Id, model.Attachment);

        await _auditService.LogAsync(userId, "Note.Created", "Note", note.Id.ToString(),
            $"Title={note.Title} Visibility={note.Visibility}");

        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    // ── GET /Notes/Edit/5 ─────────────────────────────────────────────────────
    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var note = await FindOwnedNoteAsync(id);
        if (note is null) return Forbid();

        return View(new EditNoteViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            Visibility = note.Visibility
        });
    }

    // ── POST /Notes/Edit ──────────────────────────────────────────────────────
    [HttpPost]
    public async Task<IActionResult> Edit(EditNoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        // Trust boundary: re-verify ownership in POST (double-submit / CSRF protection)
        var note = await FindOwnedNoteAsync(model.Id);
        if (note is null) return Forbid();

        note.Title = model.Title.Trim();
        note.Content = model.Content;
        note.Visibility = model.Visibility;
        note.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();

        if (model.NewAttachment is not null)
            await SaveAttachmentAsync(note.Id, model.NewAttachment);

        await _auditService.LogAsync(_userManager.GetUserId(User), "Note.Updated", "Note", note.Id.ToString());
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    // ── GET /Notes/Delete/5 ───────────────────────────────────────────────────
    [HttpGet]
    public async Task<IActionResult> Delete(int id)
    {
        var note = await FindOwnedNoteAsync(id);
        if (note is null) return Forbid();
        return View(new NoteListItemViewModel { Id = note.Id, Title = note.Title });
    }

    // ── POST /Notes/Delete ────────────────────────────────────────────────────
    [HttpPost, ActionName("Delete")]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var note = await _db.Notes
            .Include(n => n.Attachments)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note is null) return NotFound();

        // Authorization: owner or admin only
        var userId = _userManager.GetUserId(User)!;
        var isAdmin = User.IsInRole("Admin");

        if (note.UserId != userId && !isAdmin) return Forbid();

        // Resilience: delete physical files before removing DB record
        foreach (var attachment in note.Attachments)
            await _fileStorage.DeleteAsync(attachment.StoredFileName);

        _db.Notes.Remove(note);
        await _db.SaveChangesAsync();

        await _auditService.LogAsync(userId, "Note.Deleted", "Note", note.Id.ToString(),
            $"Title={note.Title}");

        return RedirectToAction(nameof(Index));
    }

    // ── GET /Notes/Search ─────────────────────────────────────────────────────
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Search(string? q)
    {
        var model = new NoteSearchViewModel { Query = q?.Trim() ?? string.Empty };

        if (string.IsNullOrWhiteSpace(model.Query))
            return View(model);

        var userId = _userManager.GetUserId(User);

        // Integrity: EF Core parameterizes the query — no string concatenation
        model.Results = await _db.Notes
            .Include(n => n.User)
            .Where(n =>
                (n.UserId == userId || n.Visibility == NoteVisibility.Public) &&
                (n.Title.Contains(model.Query) || n.Content.Contains(model.Query)))
            .OrderByDescending(n => n.CreatedAt)
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > ExcerptLength
                    ? n.Content.Substring(0, ExcerptLength) + "…"
                    : n.Content,
                Visibility = n.Visibility,
                AuthorUserName = n.User!.UserName!,
                CreatedAt = n.CreatedAt
            })
            .ToListAsync();

        return View(model);
    }

    // ── GET /Notes/TopRated ───────────────────────────────────────────────────
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> TopRated()
    {
        var results = await _db.Notes
            .Include(n => n.User)
            .Where(n => n.Visibility == NoteVisibility.Public && n.Ratings.Count >= 3)
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > ExcerptLength
                    ? n.Content.Substring(0, ExcerptLength) + "…"
                    : n.Content,
                AuthorUserName = n.User!.UserName!,
                CreatedAt = n.CreatedAt,
                AverageRating = n.Ratings.Average(r => r.Value),
                RatingCount = n.Ratings.Count
            })
            .OrderByDescending(n => n.AverageRating)
            .ToListAsync();

        return View(results);
    }

    // ── POST /Notes/GenerateShareLink ─────────────────────────────────────────
    [HttpPost]
    public async Task<IActionResult> GenerateShareLink(int id)
    {
        var note = await FindOwnedNoteAsync(id);
        if (note is null) return Forbid();

        // Revoke any existing active links first (Authenticity: only one active token)
        var existingLinks = await _db.ShareLinks
            .Where(s => s.NoteId == id && s.IsActive)
            .ToListAsync();

        foreach (var link in existingLinks)
            link.IsActive = false;

        var newLink = new ShareLink
        {
            NoteId = id,
            Token = _shareTokenService.GenerateToken()
        };

        _db.ShareLinks.Add(newLink);
        await _db.SaveChangesAsync();

        await _auditService.LogAsync(_userManager.GetUserId(User), "Note.ShareLinkGenerated",
            "Note", id.ToString());

        return RedirectToAction(nameof(Details), new { id });
    }

    // ── POST /Notes/RevokeShareLink ───────────────────────────────────────────
    [HttpPost]
    public async Task<IActionResult> RevokeShareLink(int id)
    {
        var note = await FindOwnedNoteAsync(id);
        if (note is null) return Forbid();

        var links = await _db.ShareLinks
            .Where(s => s.NoteId == id && s.IsActive)
            .ToListAsync();

        foreach (var link in links)
            link.IsActive = false;

        await _db.SaveChangesAsync();
        await _auditService.LogAsync(_userManager.GetUserId(User), "Note.ShareLinkRevoked",
            "Note", id.ToString());

        return RedirectToAction(nameof(Details), new { id });
    }

    // ── POST /Notes/Rate ──────────────────────────────────────────────────────
    [HttpPost]
    public async Task<IActionResult> Rate(RatingInputViewModel model)
    {
        if (!ModelState.IsValid)
            return RedirectToAction(nameof(Details), new { id = model.NoteId });

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note is null) return NotFound();

        // Integrity: upsert — one rating per user per note
        var existing = await _db.Ratings
            .FirstOrDefaultAsync(r => r.NoteId == model.NoteId && r.UserId == userId);

        if (existing is null)
        {
            _db.Ratings.Add(new Rating
            {
                NoteId = model.NoteId,
                UserId = userId,
                Value = model.Value,
                Comment = model.Comment
            });
        }
        else
        {
            existing.Value = model.Value;
            existing.Comment = model.Comment;
            existing.UpdatedAt = DateTime.UtcNow;
        }

        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Details), new { id = model.NoteId });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /// <summary>Retrieves a note only if the current user is the owner.
    /// Returns null on not-found or unauthorized — callers return Forbid().</summary>
    private async Task<Note?> FindOwnedNoteAsync(int noteId)
    {
        var userId = _userManager.GetUserId(User);
        return await _db.Notes.FirstOrDefaultAsync(
            n => n.Id == noteId && n.UserId == userId);
    }

    private async Task SaveAttachmentAsync(int noteId, IFormFile file)
    {
        try
        {
            var stored = await _fileStorage.SaveAsync(file);
            _db.Attachments.Add(new Attachment
            {
                NoteId = noteId,
                OriginalFileName = Path.GetFileName(file.FileName), // strip path
                StoredFileName = stored.StoredFileName,
                ContentType = stored.ContentType,
                FileSizeBytes = stored.FileSizeBytes
            });
            await _db.SaveChangesAsync();
        }
        catch (InvalidOperationException ex)
        {
            // Resilience: file validation failure is non-fatal; note was already saved
            _logger.LogWarning("Attachment validation failed for note {NoteId}: {Reason}",
                noteId, ex.Message);
            TempData["AttachmentError"] = ex.Message;
        }
    }

    private static NoteDetailsViewModel MapToDetails(Note note, string? userId)
    {
        return new NoteDetailsViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            Visibility = note.Visibility,
            AuthorUserName = note.User?.UserName ?? string.Empty,
            AuthorId = note.UserId,
            CreatedAt = note.CreatedAt,
            UpdatedAt = note.UpdatedAt,
            IsOwner = note.UserId == userId,
            Attachments = note.Attachments.Select(a => new AttachmentViewModel
            {
                Id = a.Id,
                OriginalFileName = a.OriginalFileName,
                FileSizeBytes = a.FileSizeBytes,
                UploadedAt = a.UploadedAt
            }).ToList(),
            Ratings = note.Ratings.Select(r => new RatingViewModel
            {
                Id = r.Id,
                RaterUserName = r.User?.UserName ?? "Unknown",
                Value = r.Value,
                Comment = r.Comment,
                CreatedAt = r.CreatedAt
            }).OrderByDescending(r => r.CreatedAt).ToList(),
            AverageRating = note.Ratings.Any() ? note.Ratings.Average(r => r.Value) : 0,
            RatingCount = note.Ratings.Count,
            ActiveShareToken = note.ShareLinks.FirstOrDefault(s => s.IsActive)?.Token,
            CurrentUserRating = note.Ratings.FirstOrDefault(r => r.UserId == userId)?.Value
        };
    }
}
