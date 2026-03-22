using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

[Authorize]
public class NotesController : Controller
{
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

    // GET: Notes
    public async Task<IActionResult> Index()
    {
        var userId = _userManager.GetUserId(User)!;
        var notes = await _db.Notes
            .Where(n => n.OwnerId == userId)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings)
            .OrderByDescending(n => n.UpdatedAt)
            .ToListAsync();

        var items = notes.Select(n => ToListItem(n, n.Owner?.UserName ?? "")).ToList();
        return View(items);
    }

    // GET: Notes/Details/5
    [AllowAnonymous]
    public async Task<IActionResult> Details(int id)
    {
        var note = await _db.Notes
            .Include(n => n.Owner)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.User)
            .Include(n => n.ShareLinks)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        var isOwner = userId != null && note.OwnerId == userId;
        var isAdmin = User.IsInRole("Admin");

        if (!note.IsPublic && !isOwner && !isAdmin)
            return Forbid();

        var avgRating = note.Ratings.Any() ? note.Ratings.Average(r => r.Stars) : 0;
        var userRating = userId != null ? note.Ratings.FirstOrDefault(r => r.UserId == userId) : null;

        var vm = new NoteDetailViewModel
        {
            Note = note,
            AverageRating = avgRating,
            CurrentUserRating = userRating,
            CanEdit = isOwner || isAdmin,
            CanDelete = isOwner || isAdmin,
            ActiveShareLinks = note.ShareLinks.Where(s => s.IsActive).ToList()
        };

        return View(vm);
    }

    // GET: Notes/Create
    public IActionResult Create() => View(new CreateNoteViewModel());

    // POST: Notes/Create
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(CreateNoteViewModel model)
    {
        // Validate files
        foreach (var file in model.Files)
        {
            if (!_fileStorage.IsAllowedExtension(file.FileName))
                ModelState.AddModelError("Files", $"File '{file.FileName}' has an unsupported extension.");
            if (!_fileStorage.IsWithinSizeLimit(file.Length))
                ModelState.AddModelError("Files", $"File '{file.FileName}' exceeds the 10MB size limit.");
        }

        if (!ModelState.IsValid) return View(model);

        var userId = _userManager.GetUserId(User)!;
        var note = new Note
        {
            Title = model.Title,
            Content = model.Content,
            IsPublic = model.IsPublic,
            OwnerId = userId
        };

        _db.Notes.Add(note);
        await _db.SaveChangesAsync();

        await SaveAttachmentsAsync(note.Id, model.Files);
        _logger.LogInformation("Note created: {NoteId} by {UserId}", note.Id, userId);

        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    // GET: Notes/Edit/5
    public async Task<IActionResult> Edit(int id)
    {
        var note = await _db.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        if (!CanModify(note, userId)) return Forbid();

        return View(new EditNoteViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic,
            ExistingAttachments = note.Attachments.ToList()
        });
    }

    // POST: Notes/Edit/5
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(int id, EditNoteViewModel model)
    {
        if (id != model.Id) return BadRequest();

        var note = await _db.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        if (!CanModify(note, userId)) return Forbid();

        foreach (var file in model.Files)
        {
            if (!_fileStorage.IsAllowedExtension(file.FileName))
                ModelState.AddModelError("Files", $"File '{file.FileName}' has an unsupported extension.");
            if (!_fileStorage.IsWithinSizeLimit(file.Length))
                ModelState.AddModelError("Files", $"File '{file.FileName}' exceeds the 10MB size limit.");
        }

        if (!ModelState.IsValid)
        {
            model.ExistingAttachments = note.Attachments.ToList();
            return View(model);
        }

        note.Title = model.Title;
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();
        await SaveAttachmentsAsync(note.Id, model.Files);

        _logger.LogInformation("Note updated: {NoteId} by {UserId}", note.Id, userId);
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    // GET: Notes/Delete/5
    public async Task<IActionResult> Delete(int id)
    {
        var note = await _db.Notes.Include(n => n.Owner).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        if (!CanModify(note, userId)) return Forbid();

        return View(note);
    }

    // POST: Notes/Delete/5
    [HttpPost, ActionName("Delete")]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var note = await _db.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        if (!CanModify(note, userId)) return Forbid();

        // Delete physical files
        foreach (var att in note.Attachments)
            _fileStorage.DeleteFile(att.StoredFileName);

        _db.Notes.Remove(note);
        await _db.SaveChangesAsync();

        _logger.LogInformation("Note deleted: {NoteId} by {UserId}", id, userId);
        return RedirectToAction(nameof(Index));
    }

    // GET: Notes/Search
    [AllowAnonymous]
    public async Task<IActionResult> Search(string? q)
    {
        var vm = new SearchViewModel { Query = q ?? string.Empty };
        if (string.IsNullOrWhiteSpace(q)) return View(vm);

        var userId = _userManager.GetUserId(User);
        var lower = q.ToLower();

        var notes = await _db.Notes
            .Include(n => n.Owner)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings)
            .Where(n =>
                (n.Title.ToLower().Contains(lower) || n.Content.ToLower().Contains(lower)) &&
                (n.IsPublic || n.OwnerId == userId))
            .OrderByDescending(n => n.CreatedAt)
            .ToListAsync();

        vm.Results = notes.Select(n => ToListItem(n, n.Owner?.UserName ?? "")).ToList();
        return View(vm);
    }

    // GET: Notes/TopRated
    [AllowAnonymous]
    public async Task<IActionResult> TopRated()
    {
        var notes = await _db.Notes
            .Include(n => n.Owner)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings)
            .Where(n => n.IsPublic && n.Ratings.Count >= 3)
            .ToListAsync();

        var items = notes
            .Select(n => ToListItem(n, n.Owner?.UserName ?? ""))
            .Where(n => n.RatingCount >= 3)
            .OrderByDescending(n => n.AverageRating)
            .ToList();

        return View(new TopRatedViewModel { Notes = items });
    }

    // POST: Notes/AddRating
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> AddRating(CreateRatingViewModel model)
    {
        if (!ModelState.IsValid)
            return RedirectToAction(nameof(Details), new { id = model.NoteId });

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note == null) return NotFound();

        // Check if user already rated
        var existing = await _db.Ratings.FirstOrDefaultAsync(r => r.NoteId == model.NoteId && r.UserId == userId);
        if (existing != null)
        {
            TempData["Error"] = "You have already rated this note. Edit your existing rating.";
            return RedirectToAction(nameof(Details), new { id = model.NoteId });
        }

        var rating = new Rating
        {
            NoteId = model.NoteId,
            UserId = userId,
            Stars = model.Stars,
            Comment = model.Comment
        };

        _db.Ratings.Add(rating);
        await _db.SaveChangesAsync();

        return RedirectToAction(nameof(Details), new { id = model.NoteId });
    }

    // POST: Notes/EditRating
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> EditRating(EditRatingViewModel model)
    {
        var userId = _userManager.GetUserId(User)!;
        var rating = await _db.Ratings.FindAsync(model.Id);

        if (rating == null) return NotFound();
        if (rating.UserId != userId) return Forbid();

        if (!ModelState.IsValid)
            return RedirectToAction(nameof(Details), new { id = rating.NoteId });

        rating.Stars = model.Stars;
        rating.Comment = model.Comment;
        rating.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();
        return RedirectToAction(nameof(Details), new { id = rating.NoteId });
    }

    // POST: Notes/DeleteRating/5
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteRating(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var rating = await _db.Ratings.FindAsync(id);

        if (rating == null) return NotFound();
        if (rating.UserId != userId) return Forbid();

        var noteId = rating.NoteId;
        _db.Ratings.Remove(rating);
        await _db.SaveChangesAsync();

        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    // POST: Notes/GenerateShareLink/5
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> GenerateShareLink(int id)
    {
        var note = await _db.Notes.FindAsync(id);
        if (note == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        if (note.OwnerId != userId) return Forbid();

        var link = new ShareLink
        {
            NoteId = id,
            Token = Guid.NewGuid().ToString("N")
        };

        _db.ShareLinks.Add(link);
        await _db.SaveChangesAsync();

        _logger.LogInformation("Share link generated for note {NoteId} by {UserId}", id, userId);
        TempData["Success"] = $"Share link created.";
        return RedirectToAction(nameof(Details), new { id });
    }

    // POST: Notes/RevokeShareLink/5
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLink(int id)
    {
        var link = await _db.ShareLinks.Include(s => s.Note).FirstOrDefaultAsync(s => s.Id == id);
        if (link == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        if (link.Note.OwnerId != userId) return Forbid();

        var noteId = link.NoteId;
        link.IsActive = false;
        await _db.SaveChangesAsync();

        _logger.LogInformation("Share link {LinkId} revoked by {UserId}", id, userId);
        return RedirectToAction(nameof(Details), new { id = noteId });
    }

    // POST: Notes/DeleteAttachment/5
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteAttachment(int id)
    {
        var att = await _db.Attachments.Include(a => a.Note).FirstOrDefaultAsync(a => a.Id == id);
        if (att == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        if (!CanModify(att.Note, userId)) return Forbid();

        var noteId = att.NoteId;
        _fileStorage.DeleteFile(att.StoredFileName);
        _db.Attachments.Remove(att);
        await _db.SaveChangesAsync();

        return RedirectToAction(nameof(Edit), new { id = noteId });
    }

    // GET: Notes/DownloadAttachment/5
    [AllowAnonymous]
    public async Task<IActionResult> DownloadAttachment(int id)
    {
        var att = await _db.Attachments.Include(a => a.Note).FirstOrDefaultAsync(a => a.Id == id);
        if (att == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        var isOwner = userId != null && att.Note.OwnerId == userId;
        if (!att.Note.IsPublic && !isOwner && !User.IsInRole("Admin"))
            return Forbid();

        var path = _fileStorage.GetFilePath(att.StoredFileName);
        if (!System.IO.File.Exists(path)) return NotFound();

        return PhysicalFile(path, att.ContentType, att.OriginalFileName);
    }

    // -- Helpers --

    private bool CanModify(Note note, string? userId)
        => userId != null && (note.OwnerId == userId || User.IsInRole("Admin"));

    private static NoteListItemViewModel ToListItem(Note n, string authorName)
    {
        var avg = n.Ratings.Any() ? n.Ratings.Average(r => r.Stars) : 0;
        return new NoteListItemViewModel
        {
            Id = n.Id,
            Title = n.Title,
            Excerpt = n.Content.Length > 200 ? n.Content[..200] + "…" : n.Content,
            AuthorName = authorName,
            CreatedAt = n.CreatedAt,
            IsPublic = n.IsPublic,
            AverageRating = Math.Round(avg, 1),
            RatingCount = n.Ratings.Count,
            AttachmentCount = n.Attachments.Count
        };
    }

    private async Task SaveAttachmentsAsync(int noteId, List<IFormFile> files)
    {
        foreach (var file in files)
        {
            if (file.Length == 0) continue;
            var (stored, ct) = await _fileStorage.SaveFileAsync(file);
            _db.Attachments.Add(new Attachment
            {
                NoteId = noteId,
                OriginalFileName = file.FileName,
                StoredFileName = stored,
                ContentType = ct,
                FileSize = file.Length
            });
        }
        await _db.SaveChangesAsync();
    }
}
