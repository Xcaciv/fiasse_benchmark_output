using LooseNotes.Data;
using LooseNotes.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Unauthenticated share link access. Token is validated server-side on every request.
/// Revoked tokens return 404 immediately - no caching delay (ASVS V8.2.2).
/// Response excludes all user PII beyond note content and metadata (Confidentiality).
/// </summary>
public sealed class ShareController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly IAuditService _auditService;
    private readonly ILogger<ShareController> _logger;

    public ShareController(
        ApplicationDbContext db,
        IAuditService auditService,
        ILogger<ShareController> logger)
    {
        _db = db;
        _auditService = auditService;
        _logger = logger;
    }

    [HttpGet]
    [EnableRateLimiting("shareView")]
    public async Task<IActionResult> View(string token)
    {
        if (string.IsNullOrWhiteSpace(token))
            return NotFound();

        // Server-side token lookup - authoritative (FIASSE S2.4)
        var shareLink = await _db.ShareLinks
            .Include(s => s.Note)
                .ThenInclude(n => n.Attachments)
            .FirstOrDefaultAsync(s => s.Token == token);

        if (shareLink == null || shareLink.IsRevoked)
        {
            // Return 404 regardless of whether token exists - prevents enumeration
            return NotFound();
        }

        await _auditService.LogAsync(
            AuditEventTypes.ShareLinkAccessed,
            null, null, GetClientIp(),
            resourceType: "sharelink", resourceId: shareLink.Id.ToString(),
            details: $"note:{shareLink.NoteId}");

        // Response excludes user PII - no email, no user ID visible in response (Confidentiality)
        var note = shareLink.Note;
        var viewModel = new SharedNoteViewModel
        {
            NoteId = note.Id,
            Title = note.Title,
            Content = note.Content,
            CreatedAt = note.CreatedAt,
            UpdatedAt = note.UpdatedAt,
            Attachments = note.Attachments.Select(a => new SharedAttachmentViewModel
            {
                Id = a.Id,
                OriginalFileName = a.OriginalFileName,
                ContentType = a.ContentType,
                FileSizeBytes = a.FileSizeBytes
            }).ToList(),
            ShareToken = token
        };

        return View(viewModel);
    }

    [HttpGet]
    [EnableRateLimiting("shareView")]
    public async Task<IActionResult> DownloadAttachment(int attachmentId, string token)
    {
        if (string.IsNullOrWhiteSpace(token))
            return NotFound();

        var shareLink = await _db.ShareLinks
            .Include(s => s.Note).ThenInclude(n => n.Attachments)
            .FirstOrDefaultAsync(s => s.Token == token && !s.IsRevoked);

        if (shareLink == null)
            return NotFound();

        var attachment = shareLink.Note.Attachments.FirstOrDefault(a => a.Id == attachmentId);
        if (attachment == null) return NotFound();

        var fileStorage = HttpContext.RequestServices.GetRequiredService<IFileStorageService>();
        var filePath = fileStorage.GetFilePath(attachment.StoredFileName);

        if (!System.IO.File.Exists(filePath))
            return NotFound();

        var fileBytes = await System.IO.File.ReadAllBytesAsync(filePath);

        // Content-Disposition: attachment prevents browser execution (ASVS V5.2.6)
        Response.Headers["Content-Disposition"] =
            $"attachment; filename=\"{Uri.EscapeDataString(attachment.OriginalFileName)}\"";

        return File(fileBytes, attachment.ContentType);
    }

    private string GetClientIp()
        => HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";
}

public sealed class SharedNoteViewModel
{
    public int NoteId { get; set; }
    public string Title { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime UpdatedAt { get; set; }
    public IList<SharedAttachmentViewModel> Attachments { get; set; } = [];
    public string ShareToken { get; set; } = string.Empty;
}

public sealed class SharedAttachmentViewModel
{
    public int Id { get; set; }
    public string OriginalFileName { get; set; } = string.Empty;
    public string ContentType { get; set; } = string.Empty;
    public long FileSizeBytes { get; set; }
}
