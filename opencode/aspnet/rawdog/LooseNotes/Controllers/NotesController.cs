using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;

namespace LooseNotes.Controllers;

public class NotesController : Controller
{
    private readonly ApplicationDbContext _context;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IFileService _fileService;
    private readonly IShareLinkService _shareLinkService;
    private readonly IActivityLogService _activityLog;

    public NotesController(
        ApplicationDbContext context,
        UserManager<ApplicationUser> userManager,
        IFileService fileService,
        IShareLinkService shareLinkService,
        IActivityLogService activityLog)
    {
        _context = context;
        _userManager = userManager;
        _fileService = fileService;
        _shareLinkService = shareLinkService;
        _activityLog = activityLog;
    }

    [Authorize]
    public async Task<IActionResult> Index()
    {
        var userId = _userManager.GetUserId(User);
        var notes = await _context.Notes
            .Include(n => n.Ratings)
            .Include(n => n.Attachments)
            .Where(n => n.UserId == userId)
            .OrderByDescending(n => n.UpdatedAt)
            .ToListAsync();

        return View(notes);
    }

    [HttpGet]
    [Authorize]
    public IActionResult Create()
    {
        return View();
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(CreateNoteViewModel model, List<IFormFile>? files)
    {
        if (ModelState.IsValid)
        {
            var userId = _userManager.GetUserId(User);
            var note = new Note
            {
                Title = model.Title,
                Content = model.Content,
                IsPublic = model.IsPublic,
                UserId = userId!,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };

            _context.Notes.Add(note);
            await _context.SaveChangesAsync();

            if (files != null && files.Count > 0)
            {
                foreach (var file in files)
                {
                    if (file.Length > 0 && _fileService.IsValidFile(file))
                    {
                        await _fileService.SaveFileAsync(file, note.Id);
                    }
                }
            }

            await _activityLog.LogAsync("Note Created", userId, $"Note '{note.Title}' created", "Note", note.Id);
            return RedirectToAction("Details", new { id = note.Id });
        }

        return View(model);
    }

    public async Task<IActionResult> Details(int id, bool? shared)
    {
        Note? note;

        if (shared == true)
        {
            var token = Request.Query["token"].FirstOrDefault();
            if (!string.IsNullOrEmpty(token))
            {
                note = await _shareLinkService.GetNoteByShareTokenAsync(token);
                if (note == null)
                {
                    return NotFound("Invalid or expired share link.");
                }
            }
            else
            {
                return NotFound();
            }
        }
        else
        {
            note = await _context.Notes
                .Include(n => n.User)
                .Include(n => n.Ratings)
                .ThenInclude(r => r.User)
                .Include(n => n.Attachments)
                .FirstOrDefaultAsync(n => n.Id == id);

            if (note == null)
                return NotFound();

            var userId = _userManager.GetUserId(User);
            var isAdmin = User.IsInRole("Admin");

            if (!note.IsPublic && note.UserId != userId && !isAdmin)
                return Forbid();
        }

        var viewModel = new NoteViewModel
        {
            Id = note!.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic,
            CreatedAt = note.CreatedAt,
            UpdatedAt = note.UpdatedAt,
            UserName = note.User?.UserName,
            AverageRating = note.AverageRating,
            RatingCount = note.Ratings.Count,
            Attachments = note.Attachments.ToList(),
            Ratings = note.Ratings.OrderByDescending(r => r.CreatedAt).Select(r => new RatingViewModel
            {
                Id = r.Id,
                Value = r.Value,
                Comment = r.Comment,
                UserName = r.User?.UserName ?? "Unknown",
                CreatedAt = r.CreatedAt
            }).ToList()
        };

        ViewBag.IsOwner = note.UserId == _userManager.GetUserId(User) || User.IsInRole("Admin");
        ViewBag.CanRate = User.Identity?.IsAuthenticated == true && !ViewBag.IsOwner;

        return View(viewModel);
    }

    [HttpGet]
    [Authorize]
    public async Task<IActionResult> Edit(int id)
    {
        var note = await _context.Notes.FindAsync(id);
        if (note == null)
            return NotFound();

        var userId = _userManager.GetUserId(User);
        if (note.UserId != userId && !User.IsInRole("Admin"))
            return Forbid();

        var model = new EditNoteViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic
        };

        return View(model);
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(EditNoteViewModel model, List<IFormFile>? files)
    {
        if (ModelState.IsValid)
        {
            var note = await _context.Notes.FindAsync(model.Id);
            if (note == null)
                return NotFound();

            var userId = _userManager.GetUserId(User);
            if (note.UserId != userId && !User.IsInRole("Admin"))
                return Forbid();

            note.Title = model.Title;
            note.Content = model.Content;
            note.IsPublic = model.IsPublic;
            note.UpdatedAt = DateTime.UtcNow;

            if (files != null && files.Count > 0)
            {
                foreach (var file in files)
                {
                    if (file.Length > 0 && _fileService.IsValidFile(file))
                    {
                        await _fileService.SaveFileAsync(file, note.Id);
                    }
                }
            }

            await _context.SaveChangesAsync();
            await _activityLog.LogAsync("Note Updated", userId, $"Note '{note.Title}' updated", "Note", note.Id);

            return RedirectToAction("Details", new { id = note.Id });
        }

        return View(model);
    }

    [HttpGet]
    [Authorize]
    public async Task<IActionResult> Delete(int id)
    {
        var note = await _context.Notes
            .Include(n => n.User)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note == null)
            return NotFound();

        var userId = _userManager.GetUserId(User);
        if (note.UserId != userId && !User.IsInRole("Admin"))
            return Forbid();

        return View(note);
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteConfirmed(int id)
    {
        var note = await _context.Notes.FindAsync(id);
        if (note == null)
            return NotFound();

        var userId = _userManager.GetUserId(User);
        if (note.UserId != userId && !User.IsInRole("Admin"))
            return Forbid();

        var attachments = await _context.Attachments.Where(a => a.NoteId == id).ToListAsync();
        foreach (var attachment in attachments)
        {
            await _fileService.DeleteFileAsync(attachment.Id);
        }

        _context.Notes.Remove(note);
        await _context.SaveChangesAsync();

        await _activityLog.LogAsync("Note Deleted", userId, $"Note '{note.Title}' deleted", "Note", id);

        return RedirectToAction("Index");
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Rate(int noteId, CreateRatingViewModel model)
    {
        var userId = _userManager.GetUserId(User);
        if (userId == null)
            return Unauthorized();

        var note = await _context.Notes.FindAsync(noteId);
        if (note == null)
            return NotFound();

        var existingRating = await _context.Ratings
            .FirstOrDefaultAsync(r => r.NoteId == noteId && r.UserId == userId);

        if (existingRating != null)
        {
            existingRating.Value = model.Value;
            existingRating.Comment = model.Comment;
            existingRating.UpdatedAt = DateTime.UtcNow;
        }
        else
        {
            var rating = new Rating
            {
                Value = model.Value,
                Comment = model.Comment,
                UserId = userId,
                NoteId = noteId,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };
            _context.Ratings.Add(rating);
        }

        await _context.SaveChangesAsync();
        await _activityLog.LogAsync("Note Rated", userId, $"Rated note {noteId}", "Note", noteId);

        return RedirectToAction("Details", new { id = noteId });
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteAttachment(int attachmentId, int noteId)
    {
        var note = await _context.Notes.FindAsync(noteId);
        if (note == null)
            return NotFound();

        var userId = _userManager.GetUserId(User);
        if (note.UserId != userId && !User.IsInRole("Admin"))
            return Forbid();

        await _fileService.DeleteFileAsync(attachmentId);

        return RedirectToAction("Edit", new { id = noteId });
    }

    [HttpGet]
    [Authorize]
    public async Task<IActionResult> Share(int id)
    {
        var note = await _context.Notes.FindAsync(id);
        if (note == null)
            return NotFound();

        var userId = _userManager.GetUserId(User);
        if (note.UserId != userId && !User.IsInRole("Admin"))
            return Forbid();

        var token = await _shareLinkService.GenerateShareLinkAsync(id);
        var shareUrl = Url.Action("Details", "Notes", new { id = id, shared = true, token }, Request.Scheme);

        var shareLinks = await _context.ShareLinks
            .Where(s => s.NoteId == id)
            .OrderByDescending(s => s.CreatedAt)
            .ToListAsync();

        ViewBag.ShareUrl = shareUrl;
        ViewBag.CurrentToken = token;

        return View(shareLinks);
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RegenerateShareLink(int id)
    {
        var note = await _context.Notes.FindAsync(id);
        if (note == null)
            return NotFound();

        var userId = _userManager.GetUserId(User);
        if (note.UserId != userId && !User.IsInRole("Admin"))
            return Forbid();

        var token = await _shareLinkService.GenerateShareLinkAsync(id);

        return RedirectToAction("Share", new { id = id });
    }

    [HttpPost]
    [Authorize]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLink(string token, int noteId)
    {
        await _shareLinkService.RevokeShareLinkAsync(token);
        return RedirectToAction("Share", new { id = noteId });
    }

    [AllowAnonymous]
    public async Task<IActionResult> TopRated()
    {
        var notes = await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .Where(n => n.IsPublic && n.Ratings.Count >= 3)
            .OrderByDescending(n => n.Ratings.Average(r => r.Value))
            .Take(20)
            .ToListAsync();

        return View(notes);
    }

    public async Task<IActionResult> Download(int id)
    {
        var attachment = await _fileService.GetFileAsync(id);
        if (attachment == null)
            return NotFound();

        var filePath = Path.Combine(Directory.GetCurrentDirectory(), "wwwroot", "uploads", attachment.StoredFileName);
        if (!System.IO.File.Exists(filePath))
            return NotFound();

        var bytes = await System.IO.File.ReadAllBytesAsync(filePath);
        return File(bytes, attachment.ContentType, attachment.FileName);
    }
}
