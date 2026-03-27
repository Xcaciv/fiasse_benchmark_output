// AttachmentsController.cs — Serves and deletes file attachments.
// Trust boundary: storedFileName is looked up via DB row — never passed by user directly.
// Integrity: access control checks note ownership/visibility before serving files.
// Confidentiality: original filenames are preserved in Content-Disposition header only.
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

[Authorize]
public sealed class AttachmentsController : Controller
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

    // ── GET /Attachments/Download/5 ───────────────────────────────────────────
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Download(int id)
    {
        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (attachment?.Note is null) return NotFound();

        // Authorization: only serve if note is public or requester owns it
        var userId = _userManager.GetUserId(User);
        if (attachment.Note.Visibility == NoteVisibility.Private &&
            attachment.Note.UserId != userId)
        {
            return Forbid();
        }

        // Trust boundary: file path derived from DB record, not user input
        var filePath = _fileStorage.GetFilePath(attachment.StoredFileName);

        if (!System.IO.File.Exists(filePath)) return NotFound();

        // Integrity: use sanitized original name in header; never execute file
        var safeOriginalName = Path.GetFileName(attachment.OriginalFileName);
        return PhysicalFile(filePath, attachment.ContentType,
            fileDownloadName: safeOriginalName);
    }

    // ── POST /Attachments/Delete/5 ────────────────────────────────────────────
    [HttpPost]
    public async Task<IActionResult> Delete(int id)
    {
        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (attachment?.Note is null) return NotFound();

        var userId = _userManager.GetUserId(User)!;

        // Authorization: only owner can delete
        if (attachment.Note.UserId != userId) return Forbid();

        await _fileStorage.DeleteAsync(attachment.StoredFileName);
        _db.Attachments.Remove(attachment);
        await _db.SaveChangesAsync();

        await _auditService.LogAsync(userId, "Attachment.Deleted", "Attachment",
            attachment.Id.ToString(), $"NoteId={attachment.NoteId}");

        return RedirectToAction("Details", "Notes", new { id = attachment.NoteId });
    }
}
