using LooseNotes.Services;
using LooseNotes.ViewModels.Notes;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Models;

namespace LooseNotes.Controllers;

[Authorize]
public class NotesController : Controller
{
    private readonly INoteService _noteService;
    private readonly IShareLinkService _shareLinkService;
    private readonly IAuditService _audit;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly ILogger<NotesController> _logger;

    public NotesController(
        INoteService noteService,
        IShareLinkService shareLinkService,
        IAuditService audit,
        UserManager<ApplicationUser> userManager,
        ILogger<NotesController> logger)
    {
        _noteService = noteService;
        _shareLinkService = shareLinkService;
        _audit = audit;
        _userManager = userManager;
        _logger = logger;
    }

    private string GetCurrentUserId() => _userManager.GetUserId(User)!;

    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var notes = await _noteService.GetUserNotesAsync(GetCurrentUserId());
        return View(notes);
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Search(string? q)
    {
        string? userId = User.Identity?.IsAuthenticated == true ? GetCurrentUserId() : null;
        var results = await _noteService.SearchNotesAsync(q ?? string.Empty, userId);
        ViewBag.Query = q;
        return View(results);
    }

    [HttpGet]
    public IActionResult Create() => View(new NoteViewModel());

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Create([Bind("Title,Content,IsPublic")] NoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var note = await _noteService.CreateNoteAsync(GetCurrentUserId(), model);
        TempData["Success"] = "Note created successfully.";
        return RedirectToAction(nameof(Detail), new { id = note.Id });
    }

    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var note = await _noteService.GetNoteForEditAsync(id, GetCurrentUserId());
        if (note is null) return NotFound();

        return View(new NoteViewModel
        {
            Title = note.Title,
            Content = note.Content,
            IsPublic = note.IsPublic
        });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Edit(int id, [Bind("Title,Content,IsPublic")] NoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var updated = await _noteService.UpdateNoteAsync(id, GetCurrentUserId(), model);
        if (!updated) return NotFound();

        TempData["Success"] = "Note updated successfully.";
        return RedirectToAction(nameof(Detail), new { id });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Delete(int id)
    {
        var deleted = await _noteService.DeleteNoteAsync(id, GetCurrentUserId());
        if (!deleted) return NotFound();

        TempData["Success"] = "Note deleted.";
        return RedirectToAction(nameof(Index));
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Detail(int id)
    {
        string? userId = User.Identity?.IsAuthenticated == true ? GetCurrentUserId() : null;
        var detail = await _noteService.GetNoteDetailAsync(id, userId);
        if (detail is null) return NotFound();
        return View(detail);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> UploadAttachment(int noteId, IFormFile file)
    {
        if (file is null || file.Length == 0)
        {
            TempData["Error"] = "No file provided.";
            return RedirectToAction(nameof(Detail), new { id = noteId });
        }

        var userId = GetCurrentUserId();
        var success = await _noteService.AddAttachmentAsync(noteId, userId, file);

        if (!success)
        {
            TempData["Error"] = "File upload failed. Check extension and size limits.";
        }
        else
        {
            _audit.LogFileEvent("ATTACHMENT_UPLOADED", userId, $"NoteId={noteId}, File={file.FileName}");
            TempData["Success"] = "Attachment uploaded.";
        }

        return RedirectToAction(nameof(Detail), new { id = noteId });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteAttachment(int attachmentId, int noteId)
    {
        var userId = GetCurrentUserId();
        var deleted = await _noteService.DeleteAttachmentAsync(attachmentId, userId);

        if (deleted)
        {
            _audit.LogFileEvent("ATTACHMENT_DELETED", userId, $"AttachmentId={attachmentId}");
            TempData["Success"] = "Attachment deleted.";
        }
        else
        {
            TempData["Error"] = "Could not delete attachment.";
        }

        return RedirectToAction(nameof(Detail), new { id = noteId });
    }

    [HttpGet]
    public async Task<IActionResult> DownloadAttachment(int id)
    {
        var userId = GetCurrentUserId();
        var attachment = await _noteService.GetAttachmentAsync(id, userId);
        if (attachment is null) return NotFound();

        var fileStorage = HttpContext.RequestServices.GetRequiredService<IFileStorageService>();
        var (stream, contentType, fileName) = await fileStorage.GetFileAsync(
            attachment.StoredFileName, attachment.OriginalFileName);

        return File(stream, contentType, fileName);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> GenerateShareLink(int noteId)
    {
        try
        {
            var shareLink = await _shareLinkService.GenerateShareLinkAsync(noteId, GetCurrentUserId());
            TempData["Success"] = "Share link generated.";
        }
        catch (UnauthorizedAccessException)
        {
            TempData["Error"] = "You do not own this note.";
        }

        return RedirectToAction(nameof(Detail), new { id = noteId });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RevokeShareLink(int noteId)
    {
        await _shareLinkService.RevokeShareLinkAsync(noteId, GetCurrentUserId());
        TempData["Success"] = "Share link revoked.";
        return RedirectToAction(nameof(Detail), new { id = noteId });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Rate([Bind("NoteId,Value,Comment")] RatingViewModel model)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Invalid rating.";
            return RedirectToAction(nameof(Detail), new { id = model.NoteId });
        }

        var success = await _noteService.AddOrUpdateRatingAsync(GetCurrentUserId(), model);
        TempData[success ? "Success" : "Error"] = success ? "Rating submitted." : "Could not submit rating.";
        return RedirectToAction(nameof(Detail), new { id = model.NoteId });
    }
}
