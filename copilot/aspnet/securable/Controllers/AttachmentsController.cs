using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services.Interfaces;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Serves file attachments through the controller to enforce access control.
/// Files are stored outside wwwroot so direct URL access is not possible (Integrity + Confidentiality).
/// </summary>
[Authorize]
[Route("[controller]/[action]")]
public class AttachmentsController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly IFileStorageService _fileStorage;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _auditService;
    private readonly ILogger<AttachmentsController> _logger;

    public AttachmentsController(
        ApplicationDbContext db,
        IFileStorageService fileStorage,
        UserManager<ApplicationUser> userManager,
        IAuditService auditService,
        ILogger<AttachmentsController> logger)
    {
        _db = db;
        _fileStorage = fileStorage;
        _userManager = userManager;
        _auditService = auditService;
        _logger = logger;
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Upload(int noteId, IFormFile file)
    {
        // Trust boundary: validate file type, size, and note ownership before persisting (Integrity)
        _logger.LogInformation("Attachment upload for note {NoteId} by {UserId}", noteId, _userManager.GetUserId(User));

        if (file is null || file.Length == 0)
        {
            TempData["Error"] = "No file selected.";
            return RedirectToAction("Details", "Notes", new { id = noteId });
        }

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.AsNoTracking().FirstOrDefaultAsync(n => n.Id == noteId && n.UserId == userId);
        if (note is null) return Forbid();

        if (!_fileStorage.IsAllowedFileType(file.FileName))
        {
            TempData["Error"] = "File type not allowed.";
            return RedirectToAction("Details", "Notes", new { id = noteId });
        }

        try
        {
            var (storedName, size) = await _fileStorage.StoreFileAsync(file);
            await SaveAttachmentRecord(noteId, file, storedName, size);
        }
        catch (ArgumentException ex)
        {
            TempData["Error"] = ex.Message;
        }

        return RedirectToAction("Details", "Notes", new { id = noteId });
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Download(int id)
    {
        // Trust boundary: verify access before streaming file bytes (Authenticity)
        _logger.LogInformation("Attachment download {AttachmentId} requested by {UserId}", id, _userManager.GetUserId(User) ?? "anonymous");

        var attachment = await _db.Attachments
            .AsNoTracking()
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (attachment?.Note is null) return NotFound();

        var userId = _userManager.GetUserId(User);
        var canAccess = attachment.Note.IsPublic || attachment.Note.UserId == userId;
        if (!canAccess) return Forbid();

        var filePath = _fileStorage.GetFilePath(attachment.StoredFileName);
        if (!System.IO.File.Exists(filePath)) return NotFound("File not found on disk.");

        return PhysicalFile(Path.GetFullPath(filePath), attachment.ContentType, attachment.OriginalFileName);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> Delete(int id)
    {
        _logger.LogInformation("Attachment delete {AttachmentId} by {UserId}", id, _userManager.GetUserId(User));

        var userId = _userManager.GetUserId(User)!;
        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id && a.Note != null && a.Note.UserId == userId);

        if (attachment is null) return Forbid();

        var noteId = attachment.NoteId;
        await _fileStorage.DeleteFileAsync(attachment.StoredFileName);
        _db.Attachments.Remove(attachment);
        await _db.SaveChangesAsync();

        return RedirectToAction("Details", "Notes", new { id = noteId });
    }

    private async Task SaveAttachmentRecord(int noteId, IFormFile file, string storedName, long size)
    {
        _db.Attachments.Add(new Attachment
        {
            NoteId = noteId,
            OriginalFileName = Path.GetFileName(file.FileName),
            StoredFileName = storedName,
            ContentType = file.ContentType,
            FileSizeBytes = size,
            UploadedAt = DateTime.UtcNow
        });
        await _db.SaveChangesAsync();
    }
}
