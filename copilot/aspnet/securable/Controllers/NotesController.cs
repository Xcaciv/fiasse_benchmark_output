using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services.Interfaces;
using LooseNotes.ViewModels.Notes;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;

namespace LooseNotes.Controllers;

/// <summary>
/// Note CRUD, sharing, and search. All state-changing actions require auth
/// and verify ownership before proceeding (Authenticity + Integrity).
/// </summary>
[Authorize]
[Route("[controller]/[action]")]
public class NotesController : Controller
{
    private readonly INoteService _noteService;
    private readonly IFileStorageService _fileStorage;
    private readonly IAuditService _auditService;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly ILogger<NotesController> _logger;
    private readonly ApplicationDbContext _db;

    public NotesController(
        INoteService noteService,
        IFileStorageService fileStorage,
        IAuditService auditService,
        UserManager<ApplicationUser> userManager,
        ILogger<NotesController> logger,
        ApplicationDbContext db)
    {
        _noteService = noteService;
        _fileStorage = fileStorage;
        _auditService = auditService;
        _userManager = userManager;
        _logger = logger;
        _db = db;
    }

    [HttpGet("/Notes")]
    public async Task<IActionResult> Index()
    {
        var userId = _userManager.GetUserId(User)!;
        _logger.LogInformation("Notes/Index called by {UserId}", userId);
        var notes = await _noteService.GetUserNotesAsync(userId);
        return View(new NoteListViewModel { Notes = notes });
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Details(int id)
    {
        var userId = _userManager.GetUserId(User);
        _logger.LogInformation("Notes/Details/{NoteId} requested by {UserId}", id, userId ?? "anonymous");

        var note = await _noteService.GetNoteAsync(id, userId);
        if (note is null) return NotFound();

        var vm = BuildDetailsViewModel(note, userId);
        return View(vm);
    }

    private NoteDetailsViewModel BuildDetailsViewModel(Note note, string? userId)
    {
        var avgRating = note.Ratings.Any() ? note.Ratings.Average(r => r.Stars) : 0;
        var activeToken = note.ShareLinks
            .FirstOrDefault(s => !s.IsRevoked && (s.ExpiresAt == null || s.ExpiresAt > DateTime.UtcNow))
            ?.Token;

        return new NoteDetailsViewModel
        {
            Note = note,
            AverageRating = Math.Round(avgRating, 1),
            RatingCount = note.Ratings.Count,
            ActiveShareToken = activeToken,
            CanEdit = note.UserId == userId,
            CanDelete = note.UserId == userId || User.IsInRole("Admin")
        };
    }

    [HttpGet]
    public IActionResult Create() => View();

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create(NoteCreateViewModel model)
    {
        // Trust boundary: full model validation before any DB or file I/O
        _logger.LogInformation("Notes/Create called by {UserId}", _userManager.GetUserId(User));

        if (!ModelState.IsValid) return View(model);

        var userId = _userManager.GetUserId(User)!;
        var note = await _noteService.CreateNoteAsync(model, userId);

        if (model.Attachments is not null && model.Attachments.Count > 0)
        {
            await ProcessAttachments(note.Id, model.Attachments);
        }

        await _auditService.LogAsync("NoteCreated", userId, $"NoteId={note.Id}", GetClientIp());
        return RedirectToAction(nameof(Details), new { id = note.Id });
    }

    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _noteService.GetNoteAsync(id, userId);
        if (note is null || note.UserId != userId) return Forbid();

        var vm = new NoteEditViewModel
        {
            Id = note.Id,
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic
        };
        return View(vm);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(int id, NoteEditViewModel model)
    {
        _logger.LogInformation("Notes/Edit/{NoteId} called by {UserId}", id, _userManager.GetUserId(User));

        if (!ModelState.IsValid) return View(model);

        var userId = _userManager.GetUserId(User)!;
        var updated = await _noteService.UpdateNoteAsync(id, model, userId);

        if (!updated) return Forbid();

        if (model.NewAttachments is not null && model.NewAttachments.Count > 0)
        {
            await ProcessAttachments(id, model.NewAttachments);
        }

        await _auditService.LogAsync("NoteUpdated", userId, $"NoteId={id}", GetClientIp());
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Delete(int id)
    {
        _logger.LogInformation("Notes/Delete/{NoteId} called by {UserId}", id, _userManager.GetUserId(User));

        var userId = _userManager.GetUserId(User)!;
        var isAdmin = User.IsInRole("Admin");
        var deleted = await _noteService.DeleteNoteAsync(id, userId, isAdmin);

        if (!deleted) return Forbid();

        await _auditService.LogAsync("NoteDeleted", userId, $"NoteId={id}", GetClientIp());
        return RedirectToAction(nameof(Index));
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> GenerateShareLink(int id, [FromServices] IShareLinkService shareLinkService)
    {
        var userId = _userManager.GetUserId(User)!;

        try
        {
            var link = await shareLinkService.CreateShareLinkAsync(id, userId);
            TempData["ShareUrl"] = Url.Action("ViewNote", "Share", new { token = link.Token }, Request.Scheme);
        }
        catch (InvalidOperationException)
        {
            return Forbid();
        }

        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLink(int id, [FromServices] IShareLinkService shareLinkService)
    {
        var userId = _userManager.GetUserId(User)!;
        await shareLinkService.RevokeShareLinkAsync(id, userId);
        return RedirectToAction(nameof(Details), new { id });
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Search(string? q)
    {
        if (string.IsNullOrWhiteSpace(q))
        {
            return View(new NoteSearchResultViewModel { Query = string.Empty });
        }

        var userId = _userManager.GetUserId(User);
        var results = await _noteService.SearchNotesAsync(q, userId);

        return View(new NoteSearchResultViewModel
        {
            Query = q,
            Results = results,
            TotalCount = results.Count()
        });
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> TopRated()
    {
        var notes = await _noteService.GetTopRatedNotesAsync(minRatings: 3);
        return View(new NoteListViewModel { Notes = notes });
    }

    private async Task ProcessAttachments(int noteId, IFormFileCollection files)
    {
        foreach (var file in files)
        {
            if (!_fileStorage.IsAllowedFileType(file.FileName)) continue;

            try
            {
                var (storedName, size) = await _fileStorage.StoreFileAsync(file);
                await SaveAttachmentRecord(noteId, file, storedName, size);
            }
            catch (ArgumentException ex)
            {
                _logger.LogWarning("Attachment rejected for note {NoteId}: {Reason}", noteId, ex.Message);
            }
        }
    }

    private async Task SaveAttachmentRecord(int noteId, IFormFile file, string storedName, long size)
    {
        var attachment = new Attachment
        {
            NoteId = noteId,
            OriginalFileName = Path.GetFileName(file.FileName),
            StoredFileName = storedName,
            ContentType = file.ContentType,
            FileSizeBytes = size,
            UploadedAt = DateTime.UtcNow
        };
        _db.Attachments.Add(attachment);
        await _db.SaveChangesAsync();
    }

    private string? GetClientIp() =>
        HttpContext.Connection.RemoteIpAddress?.ToString();
}
