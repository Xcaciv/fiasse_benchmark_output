using LooseNotes.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Renders a note via share link token.
/// No authentication required — access is gated solely on valid, active token.
/// Token is validated server-side; no user-supplied path components access storage.
/// </summary>
[AllowAnonymous]
public class ShareController : Controller
{
    private readonly ApplicationDbContext _db;

    public ShareController(ApplicationDbContext db)
    {
        _db = db;
    }

    /// <summary>
    /// Trust boundary: token comes from URL segment.
    /// Lookup uses parameterized EF query (Integrity).
    /// Token is treated as an opaque credential — not logged (Confidentiality).
    /// </summary>
    [HttpGet("share/{token}")]
    public async Task<IActionResult> View(string token)
    {
        if (string.IsNullOrWhiteSpace(token) || token.Length > 64)
            return NotFound();

        var shareLink = await _db.ShareLinks
            .Include(s => s.Note)
                .ThenInclude(n => n!.Owner)
            .Include(s => s.Note)
                .ThenInclude(n => n!.Attachments)
            .FirstOrDefaultAsync(s => s.Token == token && s.IsActive);

        if (shareLink is null)
            return NotFound();

        return View(shareLink.Note);
    }
}
