using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels;

namespace LooseNotes.Controllers;

[Authorize]
public class NotesController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IFileStorageService _fileStorage;
    private readonly ILogger<NotesController> _logger;

    private static readonly string[] AllowedExtensions = { ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg" };

    public NotesController(ApplicationDbContext db, UserManager<ApplicationUser> userManager,
        IFileStorageService fileStorage, ILogger<NotesController> logger)
    {
        _db = db;
        _userManager = userManager;
        _fileStorage = fileStorage;
        _logger = logger;
    }

    public async Task<IActionResult> Index()
    {
        var userId = _userManager.GetUserId(User)!;
        var notes = await _db.Notes
            .Where(n => n.UserId == userId)
            .Include(n => n.Ratings)
            .OrderByDescending(n => n.CreatedAt)
            .ToListAsync();
        return View(notes);
    }

    [HttpGet]
    public IActionResult Create() => View(new CreateNoteViewModel());

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(CreateNoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var userId = _userManager.GetUserId(User)!;
        var note = new Note
        {
            Title = model.Title,
            Content = model.Content,
            IsPublic = model.IsPublic,
            UserId = userId
        };
        _db.Notes.Add(note);
        await _db.SaveChangesAsync();

        foreach (var file in model.Attachments)
            await SaveAttachmentAsync(file, note.Id);

        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    public async Task<IActionResult> Details(int id)
    {
        var note = await _db.Notes
            .Include(n => n.User)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.User)
            .Include(n => n.ShareLinks)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        var isOwner = note.UserId == userId;

        if (!note.IsPublic && !isOwner && !User.IsInRole("Admin"))
            return Forbid();

        var vm = new NoteDetailsViewModel
        {
            Note = note,
            CanEdit = isOwner || User.IsInRole("Admin"),
            CurrentUserRating = note.Ratings.FirstOrDefault(r => r.UserId == userId),
            ShareToken = note.ShareLinks.FirstOrDefault()?.Token
        };
        return View(vm);
    }

    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var note = await _db.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User)!;
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        return View(new EditNoteViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic,
            ExistingAttachments = note.Attachments.ToList()
        });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(EditNoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var note = await _db.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == model.Id);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User)!;
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        note.Title = model.Title;
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.UpdatedAt = DateTime.UtcNow;

        foreach (var file in model.NewAttachments)
            await SaveAttachmentAsync(file, note.Id);

        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [HttpGet]
    public async Task<IActionResult> Delete(int id)
    {
        var note = await _db.Notes.Include(n => n.User).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User)!;
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        return View(note);
    }

    [HttpPost, ActionName("Delete"), ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var note = await _db.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User)!;
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        foreach (var att in note.Attachments)
            _fileStorage.DeleteFile(att.StoredFileName);

        _db.Notes.Remove(note);
        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Index));
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteAttachment(int attachmentId, int noteId)
    {
        var att = await _db.Attachments.FindAsync(attachmentId);
        if (att == null) return NotFound();

        var note = await _db.Notes.FindAsync(att.NoteId);
        var userId = _userManager.GetUserId(User)!;
        if (note == null || (note.UserId != userId && !User.IsInRole("Admin"))) return Forbid();

        _fileStorage.DeleteFile(att.StoredFileName);
        _db.Attachments.Remove(att);
        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Edit), new { id = noteId });
    }

    public async Task<IActionResult> Download(int id)
    {
        var att = await _db.Attachments.Include(a => a.Note).FirstOrDefaultAsync(a => a.Id == id);
        if (att == null) return NotFound();

        var userId = _userManager.GetUserId(User)!;
        var isOwner = att.Note!.UserId == userId;
        if (!att.Note.IsPublic && !isOwner && !User.IsInRole("Admin")) return Forbid();

        var path = _fileStorage.GetFilePath(att.StoredFileName);
        if (!System.IO.File.Exists(path)) return NotFound();

        var bytes = await System.IO.File.ReadAllBytesAsync(path);
        return File(bytes, att.ContentType, att.FileName);
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Rate(RatingViewModel model)
    {
        if (!ModelState.IsValid) return BadRequest();

        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User)!;
        var existing = await _db.Ratings.FirstOrDefaultAsync(r => r.NoteId == model.NoteId && r.UserId == userId);

        if (existing != null)
        {
            existing.Stars = model.Stars;
            existing.Comment = model.Comment;
            existing.UpdatedAt = DateTime.UtcNow;
        }
        else
        {
            _db.Ratings.Add(new Rating
            {
                NoteId = model.NoteId,
                UserId = userId,
                Stars = model.Stars,
                Comment = model.Comment
            });
        }
        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Details), new { id = model.NoteId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> GenerateShareLink(int noteId)
    {
        var note = await _db.Notes.Include(n => n.ShareLinks).FirstOrDefaultAsync(n => n.Id == noteId);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User)!;
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        // Remove existing share links
        _db.ShareLinks.RemoveRange(note.ShareLinks);

        var link = new ShareLink { NoteId = noteId, Token = Guid.NewGuid().ToString("N") };
        _db.ShareLinks.Add(link);
        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLink(int noteId)
    {
        var note = await _db.Notes.Include(n => n.ShareLinks).FirstOrDefaultAsync(n => n.Id == noteId);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User)!;
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        _db.ShareLinks.RemoveRange(note.ShareLinks);
        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    [AllowAnonymous]
    public async Task<IActionResult> Search(string q = "")
    {
        var userId = _userManager.GetUserId(User);
        var query = _db.Notes.Include(n => n.User).AsQueryable();

        if (!string.IsNullOrWhiteSpace(q))
        {
            var lower = q.ToLower();
            query = query.Where(n =>
                n.Title.ToLower().Contains(lower) ||
                n.Content.ToLower().Contains(lower));
        }

        // Only show: own notes + public notes from others
        query = query.Where(n => n.UserId == userId || n.IsPublic);

        var results = await query.OrderByDescending(n => n.CreatedAt).ToListAsync();
        return View(new SearchViewModel { Query = q, Results = results });
    }

    [AllowAnonymous]
    public async Task<IActionResult> TopRated()
    {
        var items = await _db.Notes
            .Where(n => n.IsPublic)
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .ToListAsync();

        var ranked = items
            .Where(n => n.Ratings.Count >= 3)
            .Select(n => (Note: n, Avg: n.Ratings.Average(r => r.Stars), Count: n.Ratings.Count))
            .OrderByDescending(x => x.Avg)
            .ToList();

        return View(new TopRatedViewModel { Items = ranked });
    }

    private async Task SaveAttachmentAsync(IFormFile file, int noteId)
    {
        var ext = Path.GetExtension(file.FileName).ToLowerInvariant();
        if (!AllowedExtensions.Contains(ext)) return;

        var stored = await _fileStorage.SaveFileAsync(file);
        _db.Attachments.Add(new Attachment
        {
            NoteId = noteId,
            FileName = file.FileName,
            StoredFileName = stored,
            ContentType = file.ContentType,
            FileSize = file.Length
        });
    }
}
