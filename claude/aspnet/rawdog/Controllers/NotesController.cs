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
    private static readonly string[] AllowedExtensions = [".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg"];
    private const long MaxFileSize = 10 * 1024 * 1024; // 10 MB

    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IFileStorageService _fileStorage;
    private readonly ILogger<NotesController> _logger;

    public NotesController(
        ApplicationDbContext db,
        UserManager<ApplicationUser> userManager,
        IFileStorageService fileStorage,
        ILogger<NotesController> logger)
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
            .OrderByDescending(n => n.UpdatedAt)
            .ToListAsync();
        return View(notes);
    }

    [HttpGet]
    public IActionResult Create() => View(new CreateNoteViewModel());

    [HttpPost]
    [ValidateAntiForgeryToken]
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

        await SaveAttachmentsAsync(model.Attachments, note.Id);

        _logger.LogInformation("Note created: {NoteId} by {UserId}", note.Id, userId);
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [HttpGet]
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
            IsOwner = isOwner,
            UserRating = userId != null ? note.Ratings.FirstOrDefault(r => r.UserId == userId) : null,
            ShareToken = note.ShareLinks.FirstOrDefault()?.Token
        };

        return View(vm);
    }

    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
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

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(EditNoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FirstOrDefaultAsync(n => n.Id == model.Id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        note.Title = model.Title;
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.UpdatedAt = DateTime.UtcNow;

        await SaveAttachmentsAsync(model.NewAttachments, note.Id);
        await _db.SaveChangesAsync();

        _logger.LogInformation("Note edited: {NoteId} by {UserId}", note.Id, userId);
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [HttpGet]
    public async Task<IActionResult> Delete(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.Include(n => n.User).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();
        return View(note);
    }

    [HttpPost, ActionName("Delete")]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        foreach (var attachment in note.Attachments)
            await _fileStorage.DeleteFileAsync(attachment.StoredFileName);

        _db.Notes.Remove(note);
        await _db.SaveChangesAsync();
        _logger.LogInformation("Note deleted: {NoteId} by {UserId}", id, userId);
        return RedirectToAction(nameof(Index));
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteAttachment(int attachmentId, int noteId)
    {
        var userId = _userManager.GetUserId(User)!;
        var attachment = await _db.Attachments.Include(a => a.Note).FirstOrDefaultAsync(a => a.Id == attachmentId);
        if (attachment == null) return NotFound();
        if (attachment.Note!.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        await _fileStorage.DeleteFileAsync(attachment.StoredFileName);
        _db.Attachments.Remove(attachment);
        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Edit), new { id = noteId });
    }

    [HttpGet]
    public async Task<IActionResult> DownloadAttachment(int id)
    {
        var attachment = await _db.Attachments.Include(a => a.Note).FirstOrDefaultAsync(a => a.Id == id);
        if (attachment == null) return NotFound();

        var note = attachment.Note!;
        var userId = _userManager.GetUserId(User);
        if (!note.IsPublic && note.UserId != userId && !User.IsInRole("Admin"))
            return Forbid();

        var filePath = _fileStorage.GetFilePath(attachment.StoredFileName);
        if (!System.IO.File.Exists(filePath)) return NotFound();

        var bytes = await System.IO.File.ReadAllBytesAsync(filePath);
        return File(bytes, attachment.ContentType, attachment.OriginalFileName);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> GenerateShareLink(int noteId)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.Include(n => n.ShareLinks).FirstOrDefaultAsync(n => n.Id == noteId);
        if (note == null) return NotFound();
        if (note.UserId != userId) return Forbid();

        // Remove existing share links
        _db.ShareLinks.RemoveRange(note.ShareLinks);

        var shareLink = new ShareLink
        {
            NoteId = noteId,
            Token = Guid.NewGuid().ToString("N")
        };
        _db.ShareLinks.Add(shareLink);
        await _db.SaveChangesAsync();

        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLink(int noteId)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.Include(n => n.ShareLinks).FirstOrDefaultAsync(n => n.Id == noteId);
        if (note == null) return NotFound();
        if (note.UserId != userId) return Forbid();

        _db.ShareLinks.RemoveRange(note.ShareLinks);
        await _db.SaveChangesAsync();

        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Rate(RateNoteViewModel model)
    {
        if (!ModelState.IsValid) return BadRequest();

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FirstOrDefaultAsync(n => n.Id == model.NoteId);
        if (note == null) return NotFound();

        var existing = await _db.Ratings.FirstOrDefaultAsync(r => r.NoteId == model.NoteId && r.UserId == userId);
        if (existing != null)
        {
            existing.Value = model.Value;
            existing.Comment = model.Comment;
            existing.UpdatedAt = DateTime.UtcNow;
        }
        else
        {
            _db.Ratings.Add(new Rating
            {
                NoteId = model.NoteId,
                UserId = userId,
                Value = model.Value,
                Comment = model.Comment
            });
        }

        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Details), new { id = model.NoteId });
    }

    [AllowAnonymous]
    [HttpGet]
    public async Task<IActionResult> Search(string? q)
    {
        var vm = new SearchViewModel { Query = q };
        if (string.IsNullOrWhiteSpace(q)) return View(vm);

        var userId = _userManager.GetUserId(User);
        var lowerQ = q.ToLower();

        var notes = await _db.Notes
            .Include(n => n.User)
            .Where(n => (n.IsPublic || n.UserId == userId) &&
                        (n.Title.ToLower().Contains(lowerQ) || n.Content.ToLower().Contains(lowerQ)))
            .Select(n => new NoteSearchResultViewModel
            {
                Id = n.Id,
                Title = n.Title,
                Excerpt = n.Content.Length > 200 ? n.Content.Substring(0, 200) : n.Content,
                AuthorName = n.User!.UserName ?? "Unknown",
                CreatedAt = n.CreatedAt,
                IsPublic = n.IsPublic
            })
            .ToListAsync();

        vm.Results = notes;
        return View(vm);
    }

    [AllowAnonymous]
    [HttpGet]
    public async Task<IActionResult> TopRated()
    {
        var results = await _db.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n => n.IsPublic && n.Ratings.Count >= 3)
            .Select(n => new TopRatedNoteViewModel
            {
                Id = n.Id,
                Title = n.Title,
                AuthorName = n.User!.UserName ?? "Unknown",
                AverageRating = n.Ratings.Average(r => (double)r.Value),
                RatingCount = n.Ratings.Count,
                Preview = n.Content.Length > 200 ? n.Content.Substring(0, 200) : n.Content
            })
            .OrderByDescending(n => n.AverageRating)
            .ToListAsync();

        return View(results);
    }

    private async Task SaveAttachmentsAsync(List<IFormFile> files, int noteId)
    {
        foreach (var file in files)
        {
            if (file.Length == 0 || file.Length > MaxFileSize) continue;
            var ext = Path.GetExtension(file.FileName).ToLowerInvariant();
            if (!AllowedExtensions.Contains(ext)) continue;

            var uniqueName = $"{Guid.NewGuid():N}{ext}";
            await _fileStorage.SaveFileAsync(file, uniqueName);

            _db.Attachments.Add(new Attachment
            {
                NoteId = noteId,
                OriginalFileName = file.FileName,
                StoredFileName = uniqueName,
                ContentType = file.ContentType,
                FileSize = file.Length
            });
        }
        await _db.SaveChangesAsync();
    }
}
