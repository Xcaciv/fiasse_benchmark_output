using LooseNotes.Data;
using LooseNotes.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Models;

namespace LooseNotes.Controllers;

/// <summary>
/// Serves file downloads and handles deletion.
///
/// SSEM controls:
///  - Download is gated by note visibility check (same rules as Notes/Details).
///  - Physical path is resolved by service using only the stored (GUID) filename.
///  - Content-Disposition forces download; filename comes from sanitised metadata.
///  - X-Content-Type-Options: nosniff prevents MIME sniffing (set globally in middleware).
/// </summary>
public class AttachmentsController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly IFileStorageService _fileStorage;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _audit;

    public AttachmentsController(ApplicationDbContext db, IFileStorageService fileStorage,
        UserManager<ApplicationUser> userManager, IAuditService audit)
    {
        _db = db;
        _fileStorage = fileStorage;
        _userManager = userManager;
        _audit = audit;
    }

    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> Download(int id)
    {
        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (attachment is null) return NotFound();

        var note = attachment.Note;
        var userId = _userManager.GetUserId(User);
        var isOwner = userId == note.OwnerId;
        var isAdmin = User.IsInRole(Data.DbInitializer.AdminRole);

        if (!note.IsPublic && !isOwner && !isAdmin)
            return User.Identity?.IsAuthenticated == true ? Forbid() : Challenge();

        var physPath = _fileStorage.GetPhysicalPath(attachment.StoredFileName);
        if (physPath is null) return NotFound();

        // Sanitise the original filename for Content-Disposition header
        var safeDisplayName = SanitizeFileName(attachment.OriginalFileName);

        return PhysicalFile(physPath, attachment.ContentType,
            fileDownloadName: safeDisplayName);
    }

    [HttpPost]
    [Authorize]
    public async Task<IActionResult> Delete(int id)
    {
        var attachment = await _db.Attachments
            .Include(a => a.Note)
            .FirstOrDefaultAsync(a => a.Id == id);

        if (attachment is null) return NotFound();

        var note = attachment.Note;
        var userId = _userManager.GetUserId(User);
        var isOwner = userId == note.OwnerId;
        var isAdmin = User.IsInRole(Data.DbInitializer.AdminRole);

        if (!isOwner && !isAdmin) return Forbid();

        await _fileStorage.DeleteAsync(attachment.StoredFileName);
        _db.Attachments.Remove(attachment);
        await _db.SaveChangesAsync();

        await _audit.LogAsync("AttachmentDeleted", true,
            $"AttachmentId={id} NoteId={note.Id}", userId, User.Identity?.Name);

        return RedirectToAction("Details", "Notes", new { id = note.Id });
    }

    /// <summary>Strip path characters and null bytes from display filename.</summary>
    private static string SanitizeFileName(string raw)
    {
        var name = Path.GetFileName(raw);
        // Remove null bytes and control characters
        name = new string(name.Where(c => c >= 0x20).ToArray());
        return string.IsNullOrWhiteSpace(name) ? "download" : name;
    }
}
