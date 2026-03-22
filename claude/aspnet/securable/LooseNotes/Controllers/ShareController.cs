using LooseNotes.Data;
using LooseNotes.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Serves notes via anonymous share links.
/// SSEM: Token is looked up by value; only active (non-revoked) tokens work.
/// No authentication is required – the token IS the credential.
/// </summary>
public class ShareController : Controller
{
    private readonly ApplicationDbContext _db;

    public ShareController(ApplicationDbContext db)
        => _db = db;

    [HttpGet("/share/{token}")]
    public async Task<IActionResult> View(string token)
    {
        if (string.IsNullOrWhiteSpace(token)) return BadRequest();

        var shareLink = await _db.ShareLinks
            .Include(s => s.Note)
                .ThenInclude(n => n.Owner)
            .Include(s => s.Note)
                .ThenInclude(n => n.Attachments)
            .Include(s => s.Note)
                .ThenInclude(n => n.Ratings)
            .FirstOrDefaultAsync(s => s.Token == token && !s.IsRevoked);

        if (shareLink is null) return NotFound();

        return View(shareLink.Note);
    }
}
