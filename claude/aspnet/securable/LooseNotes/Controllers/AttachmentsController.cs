using LooseNotes.Data;
using LooseNotes.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Controllers;

/// <summary>
/// Handles attachment downloads and deletions.
/// Download enforces note access rules; deletion requires ownership (Authenticity).
/// Stored filenames are never exposed to clients; access is via attachment ID (Confidentiality).
/// </summary>
[Authorize]
public class AttachmentsController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly IFileStorageService _fileStorage;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _auditService;

    public AttachmentsController(
        ApplicationDbContext db,
        IFileStorageService fileStorage,
        UserManager<ApplicationUser> userManager,
        IAuditService auditService)
    {
        _db = db;
        _fileStorage = fileStorage;
        _userManager = userManager;
        _auditService = auditService;
    }

    /// <summary>
    /// Download attachment by ID.
    /// Checks note access rules before serving file stream (Authenticity).
    /// </summary>
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Download(int id)
    {
        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (attachment is null) return NotFound();

        var userId = _userManager.GetUserId(User);
        if (!CanAccessNote(attachment.Note!, userId))
            return userId is null ? Challenge() : Forbid();

        var stream = await _fileStorage.OpenReadAsync(attachment.StoredFileName);
        if (stream is null) return NotFound();

        // Content-Disposition: attachment prevents inline execution in browsers (Integrity)
        return File(stream, attachment.ContentType,
            fileDownloadName: attachment.OriginalFileName);
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Delete(int id)
    {
        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (attachment is null) return NotFound();

        var userId = _userManager.GetUserId(User)!;
        var isAdmin = User.IsInRole(Data.DbInitializer.AdminRoleName);

        // Only note owner or admin may delete attachments (Authenticity)
        if (attachment.Note!.OwnerId != userId && !isAdmin)
            return Forbid();

        var noteId = attachment.NoteId;
        await _fileStorage.DeleteAsync(attachment.StoredFileName);
        _db.Attachments.Remove(attachment);
        await _db.SaveChangesAsync();

        await _auditService.RecordAsync("AttachmentDeleted", userId: userId,
            resourceType: "Attachment", resourceId: id.ToString());

        return RedirectToAction("Details", "Notes", new { id = noteId });
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private static bool CanAccessNote(Note note, string? userId)
        => note.IsPublic || note.OwnerId == userId;
}
