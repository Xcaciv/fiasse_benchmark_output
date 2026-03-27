using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// File upload and download. All file paths are server-generated; user input never
/// contributes to file system paths (ASVS V5.3.2).
/// Download endpoint enforces Content-Disposition: attachment to prevent browser execution (ASVS V5.2.6).
/// </summary>
[Authorize]
[AutoValidateAntiforgeryToken]
public sealed class AttachmentsController : Controller
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
    [EnableRateLimiting("upload")]
    public async Task<IActionResult> Upload(int noteId, IFormFile file)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(noteId);

        if (note == null) return NotFound();
        if (note.UserId != userId) return Forbid();

        if (file == null || file.Length == 0)
        {
            TempData["Error"] = "No file was selected.";
            return RedirectToAction("Details", "Notes", new { id = noteId });
        }

        try
        {
            var result = await _fileStorage.StoreFileAsync(file, userId, noteId);

            var attachment = new Attachment
            {
                NoteId = noteId,
                UserId = userId,
                StoredFileName = result.StoredFileName,
                OriginalFileName = result.OriginalFileName,
                ContentType = result.ContentType,
                FileSizeBytes = result.FileSizeBytes,
                UploadedAt = DateTime.UtcNow
            };

            _db.Attachments.Add(attachment);
            await _db.SaveChangesAsync();

            await _auditService.LogAsync(
                AuditEventTypes.FileUploaded,
                userId, User.Identity?.Name, GetClientIp(),
                outcome: "success",
                resourceType: "attachment", resourceId: attachment.Id.ToString(),
                details: $"note:{noteId} size:{result.FileSizeBytes}");
        }
        catch (FileValidationException ex)
        {
            await _auditService.LogAsync(
                AuditEventTypes.FileUploaded,
                userId, User.Identity?.Name, GetClientIp(),
                outcome: "rejected",
                resourceType: "attachment", resourceId: noteId.ToString(),
                details: ex.Message);

            TempData["Error"] = ex.Message;
        }

        return RedirectToAction("Details", "Notes", new { id = noteId });
    }

    [HttpGet]
    public async Task<IActionResult> Download(int id)
    {
        var userId = _userManager.GetUserId(User)!;

        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (attachment == null) return NotFound();

        // Authorization: note owner can download; public note allows any authenticated user;
        // share link access is handled by ShareController
        var isOwner = attachment.Note.UserId == userId;
        if (!isOwner && !attachment.Note.IsPublic)
            return Forbid();

        var filePath = _fileStorage.GetFilePath(attachment.StoredFileName);
        if (!System.IO.File.Exists(filePath))
        {
            _logger.LogError("Attachment file missing. AttachmentId={Id} StoredFile={File}",
                id, attachment.StoredFileName);
            return NotFound();
        }

        await _auditService.LogAsync(
            AuditEventTypes.FileDownloaded,
            userId, User.Identity?.Name, GetClientIp(),
            resourceType: "attachment", resourceId: id.ToString());

        var fileBytes = await System.IO.File.ReadAllBytesAsync(filePath);

        // Content-Disposition: attachment prevents browser from executing the file (ASVS V5.2.6)
        Response.Headers["Content-Disposition"] =
            $"attachment; filename=\"{Uri.EscapeDataString(attachment.OriginalFileName)}\"";

        return File(fileBytes, attachment.ContentType);
    }

    [HttpPost]
    public async Task<IActionResult> Delete(int id)
    {
        var userId = _userManager.GetUserId(User)!;

        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (attachment == null) return NotFound();
        if (attachment.Note.UserId != userId) return Forbid();

        var noteId = attachment.NoteId;
        var storedFileName = attachment.StoredFileName;

        _db.Attachments.Remove(attachment);
        await _db.SaveChangesAsync();

        // Delete physical file after DB record removed to avoid orphaned records on DB failure
        await _fileStorage.DeleteFileAsync(storedFileName);

        await _auditService.LogAsync(
            AuditEventTypes.FileDeleted,
            userId, User.Identity?.Name, GetClientIp(),
            resourceType: "attachment", resourceId: id.ToString(),
            details: $"note:{noteId}");

        return RedirectToAction("Details", "Notes", new { id = noteId });
    }

    private string GetClientIp()
        => HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";
}
