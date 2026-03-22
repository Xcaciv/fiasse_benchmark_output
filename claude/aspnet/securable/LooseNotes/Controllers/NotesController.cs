using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Notes;
using LooseNotes.ViewModels.Ratings;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// CRUD operations for notes plus file upload.
///
/// FIASSE / SSEM controls:
///  - [Authorize] on all mutation endpoints.
///  - Ownership verified before edit/delete (server-side, not client-side).
///  - File uploads validated in service layer (extension white-list + size limit).
///  - Searches use parameterized EF Core queries – no string concatenation into SQL.
/// </summary>
[Authorize]
public class NotesController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IFileStorageService _fileStorage;
    private readonly IAuditService _audit;

    public NotesController(ApplicationDbContext db, UserManager<ApplicationUser> userManager,
        IFileStorageService fileStorage, IAuditService audit)
    {
        _db = db;
        _userManager = userManager;
        _fileStorage = fileStorage;
        _audit = audit;
    }

    // -----------------------------------------------------------------------
    // Index – list the current user's notes
    // -----------------------------------------------------------------------
    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var userId = _userManager.GetUserId(User)!;
        var notes = await _db.Notes
            .Where(n => n.OwnerId == userId)
            .OrderByDescending(n => n.UpdatedAt)
            .Include(n => n.Ratings)
            .ToListAsync();
        return View(notes);
    }

    // -----------------------------------------------------------------------
    // Search
    // -----------------------------------------------------------------------
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Search(string? q)
    {
        var vm = new NoteSearchViewModel { Query = q };
        if (string.IsNullOrWhiteSpace(q)) return View(vm);

        var lower = q.ToLower();
        var userId = _userManager.GetUserId(User);

        // EF Core generates parameterized SQL – no injection risk
        var results = await _db.Notes
            .Include(n => n.Owner)
            .Where(n =>
                (n.Title.ToLower().Contains(lower) || n.Content.ToLower().Contains(lower))
                && (n.IsPublic || n.OwnerId == userId))
            .OrderByDescending(n => n.CreatedAt)
            .ToListAsync();

        vm.Results = results;
        return View(vm);
    }

    // -----------------------------------------------------------------------
    // Details
    // -----------------------------------------------------------------------
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Details(int id)
    {
        var note = await _db.Notes
            .Include(n => n.Owner)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.Rater)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note is null) return NotFound();

        var userId = _userManager.GetUserId(User);
        var isOwner = userId == note.OwnerId;
        var isAdmin = User.IsInRole(Data.DbInitializer.AdminRole);

        // Access control: private notes visible only to owner or admin
        if (!note.IsPublic && !isOwner && !isAdmin)
            return User.Identity?.IsAuthenticated == true ? Forbid() : Challenge();

        var vm = new NoteDetailsViewModel
        {
            Note = note,
            IsOwner = isOwner,
            IsAdmin = isAdmin,
            AverageRating = note.Ratings.Any() ? note.Ratings.Average(r => r.Stars) : 0,
            RatingCount = note.Ratings.Count,
            CurrentUserRating = userId is not null
                ? note.Ratings.FirstOrDefault(r => r.RaterId == userId)
                : null,
            ShareToken = isOwner
                ? (await _db.ShareLinks.Where(s => s.NoteId == id && !s.IsRevoked)
                                       .OrderByDescending(s => s.CreatedAt)
                                       .FirstOrDefaultAsync())?.Token
                : null
        };

        return View(vm);
    }

    // -----------------------------------------------------------------------
    // Create
    // -----------------------------------------------------------------------
    [HttpGet]
    public IActionResult Create() => View(new CreateNoteViewModel());

    [HttpPost]
    public async Task<IActionResult> Create(CreateNoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

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
        await _audit.LogAsync("NoteCreated", true, $"NoteId={note.Id}", userId, User.Identity?.Name);

        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    // -----------------------------------------------------------------------
    // Edit
    // -----------------------------------------------------------------------
    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var note = await _db.Notes.FindAsync(id);
        if (note is null) return NotFound();
        if (!CanModify(note)) return Forbid();

        return View(new EditNoteViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic
        });
    }

    [HttpPost]
    public async Task<IActionResult> Edit(EditNoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var note = await _db.Notes.FindAsync(model.Id);
        if (note is null) return NotFound();
        if (!CanModify(note)) return Forbid();

        note.Title = model.Title;
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();
        await _audit.LogAsync("NoteEdited", true, $"NoteId={note.Id}",
            _userManager.GetUserId(User), User.Identity?.Name);

        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    // -----------------------------------------------------------------------
    // Delete
    // -----------------------------------------------------------------------
    [HttpGet]
    public async Task<IActionResult> Delete(int id)
    {
        var note = await _db.Notes.Include(n => n.Owner).FirstOrDefaultAsync(n => n.Id == id);
        if (note is null) return NotFound();
        if (!CanModify(note)) return Forbid();
        return View(note);
    }

    [HttpPost, ActionName("Delete")]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var note = await _db.Notes
            .Include(n => n.Attachments)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note is null) return NotFound();
        if (!CanModify(note)) return Forbid();

        // Delete physical files
        foreach (var att in note.Attachments)
            await _fileStorage.DeleteAsync(att.StoredFileName);

        _db.Notes.Remove(note);
        await _db.SaveChangesAsync();

        await _audit.LogAsync("NoteDeleted", true, $"NoteId={id}",
            _userManager.GetUserId(User), User.Identity?.Name);

        return RedirectToAction(nameof(Index));
    }

    // -----------------------------------------------------------------------
    // File Upload
    // -----------------------------------------------------------------------
    [HttpPost]
    public async Task<IActionResult> Upload(int noteId, IFormFile? file)
    {
        if (file is null || file.Length == 0)
        {
            TempData["Error"] = "No file selected.";
            return RedirectToAction(nameof(Details), new { id = noteId });
        }

        var note = await _db.Notes.FindAsync(noteId);
        if (note is null) return NotFound();
        if (!CanModify(note)) return Forbid();

        try
        {
            var storedName = await _fileStorage.SaveAsync(file);
            var userId = _userManager.GetUserId(User)!;

            _db.Attachments.Add(new Attachment
            {
                NoteId = noteId,
                OriginalFileName = Path.GetFileName(file.FileName), // metadata only
                StoredFileName = storedName,
                ContentType = file.ContentType,
                FileSizeBytes = file.Length,
                UploadedById = userId,
                UploadedAt = DateTime.UtcNow
            });

            await _db.SaveChangesAsync();
            await _audit.LogAsync("FileUploaded", true, $"NoteId={noteId} File={storedName}",
                userId, User.Identity?.Name);
        }
        catch (InvalidOperationException ex)
        {
            TempData["Error"] = ex.Message;
        }

        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    // -----------------------------------------------------------------------
    // Share links
    // -----------------------------------------------------------------------
    [HttpPost]
    public async Task<IActionResult> GenerateShareLink(int noteId)
    {
        var note = await _db.Notes.FindAsync(noteId);
        if (note is null) return NotFound();
        if (!IsOwner(note)) return Forbid();

        // Generate cryptographically random token
        var token = Convert.ToHexString(System.Security.Cryptography.RandomNumberGenerator.GetBytes(32));

        _db.ShareLinks.Add(new ShareLink
        {
            NoteId = noteId,
            Token = token,
            CreatedAt = DateTime.UtcNow
        });

        await _db.SaveChangesAsync();
        await _audit.LogAsync("ShareLinkGenerated", true, $"NoteId={noteId}",
            _userManager.GetUserId(User), User.Identity?.Name);

        TempData["ShareLink"] = Url.Action("View", "Share", new { token }, Request.Scheme);
        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    [HttpPost]
    public async Task<IActionResult> RevokeShareLink(int noteId)
    {
        var note = await _db.Notes.FindAsync(noteId);
        if (note is null) return NotFound();
        if (!IsOwner(note)) return Forbid();

        var links = await _db.ShareLinks.Where(s => s.NoteId == noteId && !s.IsRevoked).ToListAsync();
        foreach (var l in links) l.IsRevoked = true;
        await _db.SaveChangesAsync();

        await _audit.LogAsync("ShareLinkRevoked", true, $"NoteId={noteId}",
            _userManager.GetUserId(User), User.Identity?.Name);

        TempData["Success"] = "All share links revoked.";
        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------
    private bool IsOwner(Note note) =>
        _userManager.GetUserId(User) == note.OwnerId;

    private bool CanModify(Note note) =>
        IsOwner(note) || User.IsInRole(Data.DbInitializer.AdminRole);
}
