using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.ViewModels.Notes;

namespace LooseNotes.Controllers;

[Authorize]
public class NotesController : Controller
{
    private readonly ApplicationDbContext _context;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IWebHostEnvironment _env;
    private readonly IConfiguration _config;
    private static readonly string[] AllowedExtensions = { ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg" };

    public NotesController(
        ApplicationDbContext context,
        UserManager<ApplicationUser> userManager,
        IWebHostEnvironment env,
        IConfiguration config)
    {
        _context = context;
        _userManager = userManager;
        _env = env;
        _config = config;
    }

    private async Task LogActivityAsync(string action, string entityType, string entityId, string? userId)
    {
        _context.ActivityLogs.Add(new ActivityLog
        {
            Action = action,
            EntityType = entityType,
            EntityId = entityId,
            Timestamp = DateTime.UtcNow,
            UserId = userId
        });
        await _context.SaveChangesAsync();
    }

    public async Task<IActionResult> Index(int page = 1)
    {
        var userId = _userManager.GetUserId(User);
        const int pageSize = 10;

        var query = _context.Notes
            .Include(n => n.Ratings)
            .Where(n => n.UserId == userId)
            .OrderByDescending(n => n.UpdatedAt);

        var total = await query.CountAsync();
        var notes = await query.Skip((page - 1) * pageSize).Take(pageSize).ToListAsync();

        var vm = new NoteListViewModel
        {
            Notes = notes,
            CurrentPage = page,
            TotalPages = (int)Math.Ceiling(total / (double)pageSize)
        };
        return View(vm);
    }

    [HttpGet]
    public IActionResult Create() => View(new NoteViewModel());

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(NoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var userId = _userManager.GetUserId(User)!;
        var note = new Note
        {
            Title = model.Title,
            Content = model.Content,
            IsPublic = model.IsPublic,
            UserId = userId,
            CreatedAt = DateTime.UtcNow,
            UpdatedAt = DateTime.UtcNow
        };

        _context.Notes.Add(note);
        await _context.SaveChangesAsync();

        await HandleFileUploads(model.Attachments, note.Id);
        await LogActivityAsync("Create", "Note", note.Id.ToString(), userId);

        TempData["Success"] = "Note created successfully.";
        return RedirectToAction(nameof(Index));
    }

    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var userId = _userManager.GetUserId(User);
        var note = await _context.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        var vm = new NoteViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic
        };
        ViewBag.ExistingAttachments = note.Attachments;
        return View(vm);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(int id, NoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var userId = _userManager.GetUserId(User);
        var note = await _context.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        note.Title = model.Title;
        note.Content = model.Content;
        note.IsPublic = model.IsPublic;
        note.UpdatedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        await HandleFileUploads(model.Attachments, note.Id);
        await LogActivityAsync("Edit", "Note", note.Id.ToString(), userId);

        TempData["Success"] = "Note updated successfully.";
        return RedirectToAction(nameof(Index));
    }

    [HttpGet]
    public async Task<IActionResult> Delete(int id)
    {
        var userId = _userManager.GetUserId(User);
        var note = await _context.Notes.FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();
        return View(note);
    }

    [HttpPost, ActionName("Delete")]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var userId = _userManager.GetUserId(User);
        var note = await _context.Notes.Include(n => n.Attachments).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        foreach (var att in note.Attachments)
        {
            var filePath = Path.Combine(_env.WebRootPath, "uploads", att.StoredFileName);
            if (System.IO.File.Exists(filePath))
                System.IO.File.Delete(filePath);
        }

        _context.Notes.Remove(note);
        await _context.SaveChangesAsync();
        await LogActivityAsync("Delete", "Note", id.ToString(), userId);

        TempData["Success"] = "Note deleted.";
        return RedirectToAction(nameof(Index));
    }

    [AllowAnonymous]
    public async Task<IActionResult> Details(int id)
    {
        var userId = _userManager.GetUserId(User);
        var note = await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.User)
            .Include(n => n.ShareLinks)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note == null) return NotFound();

        bool canView = note.IsPublic || note.UserId == userId || User.IsInRole("Admin");
        if (!canView) return Forbid();

        ViewBag.CurrentUserId = userId;
        ViewBag.UserRating = note.Ratings.FirstOrDefault(r => r.UserId == userId);
        return View(note);
    }

    [AllowAnonymous]
    public async Task<IActionResult> SharedView(string token)
    {
        var shareLink = await _context.ShareLinks
            .Include(s => s.Note).ThenInclude(n => n!.User)
            .Include(s => s.Note).ThenInclude(n => n!.Attachments)
            .Include(s => s.Note).ThenInclude(n => n!.Ratings)
            .FirstOrDefaultAsync(s => s.Token == token);

        if (shareLink == null || shareLink.Note == null) return NotFound();

        ViewBag.IsSharedView = true;
        return View("Details", shareLink.Note);
    }

    [HttpGet]
    public async Task<IActionResult> Share(int id)
    {
        var userId = _userManager.GetUserId(User);
        var note = await _context.Notes.Include(n => n.ShareLinks).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        var baseUrl = $"{Request.Scheme}://{Request.Host}";
        var vm = new ShareViewModel
        {
            Note = note,
            ShareLinks = note.ShareLinks.ToList(),
            BaseUrl = baseUrl
        };
        return View(vm);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RegenerateShareLink(int id)
    {
        var userId = _userManager.GetUserId(User);
        var note = await _context.Notes.FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        var link = new ShareLink
        {
            Token = Guid.NewGuid().ToString("N"),
            NoteId = id,
            CreatedAt = DateTime.UtcNow
        };
        _context.ShareLinks.Add(link);
        await _context.SaveChangesAsync();

        return RedirectToAction(nameof(Share), new { id });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLink(int id, int linkId)
    {
        var userId = _userManager.GetUserId(User);
        var note = await _context.Notes.FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();
        if (note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        var link = await _context.ShareLinks.FirstOrDefaultAsync(s => s.Id == linkId && s.NoteId == id);
        if (link != null)
        {
            _context.ShareLinks.Remove(link);
            await _context.SaveChangesAsync();
        }

        return RedirectToAction(nameof(Share), new { id });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Rate(RatingViewModel model)
    {
        if (!ModelState.IsValid) return BadRequest();

        var userId = _userManager.GetUserId(User)!;
        var note = await _context.Notes.FirstOrDefaultAsync(n => n.Id == model.NoteId);
        if (note == null) return NotFound();

        var existing = await _context.Ratings.FirstOrDefaultAsync(r => r.NoteId == model.NoteId && r.UserId == userId);
        if (existing != null)
        {
            existing.Stars = model.Stars;
            existing.Comment = model.Comment;
            existing.UpdatedAt = DateTime.UtcNow;
        }
        else
        {
            _context.Ratings.Add(new Rating
            {
                NoteId = model.NoteId,
                UserId = userId,
                Stars = model.Stars,
                Comment = model.Comment,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
        }

        await _context.SaveChangesAsync();
        TempData["Success"] = "Rating saved.";
        return RedirectToAction(nameof(Details), new { id = model.NoteId });
    }

    [AllowAnonymous]
    public async Task<IActionResult> DownloadAttachment(int id)
    {
        var attachment = await _context.Attachments.Include(a => a.Note).FirstOrDefaultAsync(a => a.Id == id);
        if (attachment == null || attachment.Note == null) return NotFound();

        var userId = _userManager.GetUserId(User);
        bool canAccess = attachment.Note.IsPublic || attachment.Note.UserId == userId || User.IsInRole("Admin");
        if (!canAccess) return Forbid();

        var filePath = Path.Combine(_env.WebRootPath, "uploads", attachment.StoredFileName);
        if (!System.IO.File.Exists(filePath)) return NotFound();

        var bytes = await System.IO.File.ReadAllBytesAsync(filePath);
        return File(bytes, attachment.ContentType, attachment.FileName);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteAttachment(int id)
    {
        var userId = _userManager.GetUserId(User);
        var attachment = await _context.Attachments.Include(a => a.Note).FirstOrDefaultAsync(a => a.Id == id);
        if (attachment == null || attachment.Note == null) return NotFound();
        if (attachment.Note.UserId != userId && !User.IsInRole("Admin")) return Forbid();

        var filePath = Path.Combine(_env.WebRootPath, "uploads", attachment.StoredFileName);
        if (System.IO.File.Exists(filePath))
            System.IO.File.Delete(filePath);

        var noteId = attachment.NoteId;
        _context.Attachments.Remove(attachment);
        await _context.SaveChangesAsync();

        return RedirectToAction(nameof(Edit), new { id = noteId });
    }

    private async Task HandleFileUploads(List<IFormFile> files, int noteId)
    {
        if (files == null || !files.Any()) return;

        var uploadPath = Path.Combine(_env.WebRootPath, "uploads");
        Directory.CreateDirectory(uploadPath);

        long maxSize = (_config.GetValue<int>("FileStorage:MaxFileSizeMB", 10)) * 1024L * 1024L;

        foreach (var file in files)
        {
            if (file.Length == 0) continue;
            if (file.Length > maxSize) continue;

            var ext = Path.GetExtension(file.FileName).ToLower();
            if (!AllowedExtensions.Contains(ext)) continue;

            var storedName = $"{Guid.NewGuid()}{ext}";
            var fullPath = Path.Combine(uploadPath, storedName);

            using var stream = new FileStream(fullPath, FileMode.Create);
            await file.CopyToAsync(stream);

            _context.Attachments.Add(new Attachment
            {
                FileName = file.FileName,
                StoredFileName = storedName,
                ContentType = file.ContentType,
                FileSize = file.Length,
                UploadedAt = DateTime.UtcNow,
                NoteId = noteId
            });
        }
        await _context.SaveChangesAsync();
    }
}
