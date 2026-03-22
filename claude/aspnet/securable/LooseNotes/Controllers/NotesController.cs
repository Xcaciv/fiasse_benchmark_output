using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Notes;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// CRUD operations for notes.
/// All mutating actions verify ownership before proceeding (Authenticity, Integrity).
/// OwnerId is always set server-side — never accepted from form data (Derived Integrity).
/// </summary>
[Authorize]
public class NotesController : Controller
{
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

    // ── List (owned notes) ────────────────────────────────────────────────────

    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var userId = _userManager.GetUserId(User)!;
        var notes = await _db.Notes
            .Where(n => n.OwnerId == userId)
            .OrderByDescending(n => n.UpdatedAt)
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > 200 ? n.Content[..200] : n.Content,
                OwnerDisplayName = n.Owner!.DisplayName,
                IsPublic = n.IsPublic,
                CreatedAt = n.CreatedAt,
                AverageRating = n.Ratings.Any() ? n.Ratings.Average(r => (double)r.Value) : null,
                RatingCount = n.Ratings.Count
            })
            .ToListAsync();

        return View(notes);
    }

    // ── Details ───────────────────────────────────────────────────────────────

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Details(int id)
    {
        var userId = _userManager.GetUserId(User);
        var note = await _db.Notes
            .Include(n => n.Owner)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.Rater)
            .Include(n => n.ShareLinks.Where(s => s.IsActive))
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note is null)
            return NotFound();

        // Access control: only owner can see private notes (Authenticity)
        if (!note.IsPublic && note.OwnerId != userId)
            return userId is null ? Challenge() : Forbid();

        var isOwner = note.OwnerId == userId;
        var userRating = userId is not null
            ? note.Ratings.FirstOrDefault(r => r.RaterId == userId)
            : null;

        var vm = BuildNoteDetailsViewModel(note, userId, isOwner, userRating);
        return View(vm);
    }

    // ── Create ─────────────────────────────────────────────────────────────────

    [HttpGet]
    public IActionResult Create() => View(new CreateNoteViewModel());

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(CreateNoteViewModel model)
    {
        if (!ModelState.IsValid)
            return View(model);

        // OwnerId from server identity — never from form data (Derived Integrity)
        var userId = _userManager.GetUserId(User)!;

        var note = new Note
        {
            Title = model.Title,
            Content = model.Content,
            IsPublic = model.IsPublic,
            OwnerId = userId,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _db.Notes.Add(note);
        await _db.SaveChangesAsync();

        if (model.Attachment is not null)
            await TrySaveAttachmentAsync(model.Attachment, note.Id, userId);

        await _auditService.RecordAsync("NoteCreated", userId: userId,
            resourceType: "Note", resourceId: note.Id.ToString());

        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    // ── Edit ───────────────────────────────────────────────────────────────────

    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(id);

        if (note is null) return NotFound();
        if (note.OwnerId != userId) return Forbid();

        var vm = new EditNoteViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic
        };
        return View(vm);
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(EditNoteViewModel model)
    {
        if (!ModelState.IsValid)
            return View(model);

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(model.Id);

        if (note is null) return NotFound();
        // Ownership re-verified on POST (Authenticity, Integrity)
        if (note.OwnerId != userId) return Forbid();

        note.Title = model.Title;
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.UpdatedAt = DateTime.UtcNow;

        if (model.NewAttachment is not null)
            await TrySaveAttachmentAsync(model.NewAttachment, note.Id, userId);

        await _db.SaveChangesAsync();
        await _auditService.RecordAsync("NoteEdited", userId: userId,
            resourceType: "Note", resourceId: note.Id.ToString());

        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    // ── Delete ─────────────────────────────────────────────────────────────────

    [HttpGet]
    public async Task<IActionResult> Delete(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var isAdmin = User.IsInRole(Data.DbInitializer.AdminRoleName);

        var note = await _db.Notes.Include(n => n.Owner).FirstOrDefaultAsync(n => n.Id == id);
        if (note is null) return NotFound();
        if (note.OwnerId != userId && !isAdmin) return Forbid();

        return View(note);
    }

    [HttpPost, ActionName("Delete"), ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var isAdmin = User.IsInRole(Data.DbInitializer.AdminRoleName);

        var note = await _db.Notes
            .Include(n => n.Attachments)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note is null) return NotFound();
        if (note.OwnerId != userId && !isAdmin) return Forbid();

        // Remove stored files before DB cascade delete (Resilience, data consistency)
        foreach (var att in note.Attachments)
            await _fileStorage.DeleteAsync(att.StoredFileName);

        _db.Notes.Remove(note);
        await _db.SaveChangesAsync();

        await _auditService.RecordAsync("NoteDeleted", userId: userId,
            resourceType: "Note", resourceId: id.ToString());

        return RedirectToAction(nameof(Index));
    }

    // ── Share Link Management ─────────────────────────────────────────────────

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> GenerateShareLink(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(id);
        if (note is null) return NotFound();
        if (note.OwnerId != userId) return Forbid();

        // Revoke old active links before creating a new one
        var existing = await _db.ShareLinks
            .Where(s => s.NoteId == id && s.IsActive)
            .ToListAsync();
        foreach (var old in existing)
            old.IsActive = false;

        var newLink = new ShareLink
        {
            NoteId = id,
            // Token generated server-side only (Derived Integrity Principle)
            Token = _shareTokenService.GenerateToken(),
            CreatedAt = DateTime.UtcNow,
            IsActive = true
        };
        _db.ShareLinks.Add(newLink);
        await _db.SaveChangesAsync();

        await _auditService.RecordAsync("ShareLinkGenerated", userId: userId,
            resourceType: "Note", resourceId: id.ToString());

        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLink(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(id);
        if (note is null) return NotFound();
        if (note.OwnerId != userId) return Forbid();

        var links = await _db.ShareLinks
            .Where(s => s.NoteId == id && s.IsActive)
            .ToListAsync();
        foreach (var link in links)
            link.IsActive = false;

        await _db.SaveChangesAsync();
        await _auditService.RecordAsync("ShareLinkRevoked", userId: userId,
            resourceType: "Note", resourceId: id.ToString());

        return RedirectToAction(nameof(Details), new { id });
    }

    // ── Search ────────────────────────────────────────────────────────────────

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Search(NoteSearchViewModel model)
    {
        if (!ModelState.IsValid || string.IsNullOrWhiteSpace(model.Query))
            return View(model);

        var userId = _userManager.GetUserId(User);
        var query = model.Query.Trim();

        // Request Surface Minimization: only extract the query param
        var results = await _db.Notes
            .Include(n => n.Owner)
            .Where(n =>
                // Own notes (any visibility) + other users' public notes
                (n.OwnerId == userId || n.IsPublic)
                && (EF.Functions.Like(n.Title, $"%{query}%")
                    || EF.Functions.Like(n.Content, $"%{query}%")))
            .OrderByDescending(n => n.UpdatedAt)
            .Take(100) // Availability: bound result set
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > 200 ? n.Content[..200] : n.Content,
                OwnerDisplayName = n.Owner!.DisplayName,
                IsPublic = n.IsPublic,
                CreatedAt = n.CreatedAt
            })
            .ToListAsync();

        model.Results = results;
        model.HasSearched = true;
        return View(model);
    }

    // ── Top Rated ─────────────────────────────────────────────────────────────

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> TopRated()
    {
        const int MinRatings = 3;
        var results = await _db.Notes
            .Include(n => n.Owner)
            .Where(n => n.IsPublic && n.Ratings.Count >= MinRatings)
            .OrderByDescending(n => n.Ratings.Average(r => (double)r.Value))
            .Take(50) // Availability: bound result set
            .Select(n => new NoteListItemViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > 200 ? n.Content[..200] : n.Content,
                OwnerDisplayName = n.Owner!.DisplayName,
                IsPublic = n.IsPublic,
                CreatedAt = n.CreatedAt,
                AverageRating = n.Ratings.Average(r => (double)r.Value),
                RatingCount = n.Ratings.Count
            })
            .ToListAsync();

        return View(results);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private async Task TrySaveAttachmentAsync(IFormFile file, int noteId, string userId)
    {
        try
        {
            var storedName = await _fileStorage.SaveAsync(file);
            var attachment = new Attachment
            {
                NoteId = noteId,
                OriginalFileName = Path.GetFileName(file.FileName), // strip directory paths
                StoredFileName = storedName,
                ContentType = file.ContentType,
                FileSizeBytes = file.Length,
                UploadedById = userId,
                UploadedAt = DateTime.UtcNow
            };
            _db.Attachments.Add(attachment);
            await _db.SaveChangesAsync();
        }
        catch (InvalidOperationException ex)
        {
            // Non-fatal: note was saved; only the attachment failed
            ModelState.AddModelError(string.Empty, ex.Message);
            _logger.LogWarning("Attachment upload rejected noteId={NoteId}: {Reason}", noteId, ex.Message);
        }
    }

    private static NoteDetailsViewModel BuildNoteDetailsViewModel(
        Note note, string? currentUserId, bool isOwner, Rating? userRating)
    {
        return new NoteDetailsViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic,
            OwnerId = note.OwnerId,
            OwnerDisplayName = note.Owner?.DisplayName ?? string.Empty,
            CreatedAt = note.CreatedAt,
            UpdatedAt = note.UpdatedAt,
            Attachments = note.Attachments.ToList(),
            Ratings = note.Ratings.Select(r => new RatingDisplayItem
            {
                Id = r.Id,
                Value = r.Value,
                Comment = r.Comment,
                RaterDisplayName = r.Rater?.DisplayName ?? "Unknown",
                RaterId = r.RaterId,
                CreatedAt = r.CreatedAt
            }).OrderByDescending(r => r.CreatedAt).ToList(),
            AverageRating = note.Ratings.Any() ? note.Ratings.Average(r => (double)r.Value) : null,
            ActiveShareToken = note.ShareLinks.FirstOrDefault(s => s.IsActive)?.Token,
            IsOwner = isOwner,
            CanRate = currentUserId is not null && !isOwner,
            UserRating = userRating is not null ? new RatingInputViewModel
            {
                NoteId = note.Id,
                RatingId = userRating.Id,
                Value = userRating.Value,
                Comment = userRating.Comment
            } : null
        };
    }
}
