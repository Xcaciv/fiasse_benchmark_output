// ShareController.cs — Serves notes via share token (unauthenticated access).
// Authenticity: token is looked up in DB — forgery yields 404.
// Availability: only one active token per note; old ones are invalidated on regeneration.
using LooseNotes.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

public sealed class ShareController : Controller
{
    private readonly ApplicationDbContext _db;

    public ShareController(ApplicationDbContext db) => _db = db;

    // ── GET /Share/View/{token} ───────────────────────────────────────────────
    [HttpGet]
    [AllowAnonymous]
    public async Task<IActionResult> View(string token)
    {
        if (string.IsNullOrWhiteSpace(token)) return NotFound();

        // Trust boundary: token must match an active link in the DB
        var shareLink = await _db.ShareLinks
            .Include(s => s.Note).ThenInclude(n => n!.User)
            .Include(s => s.Note).ThenInclude(n => n!.Attachments)
            .Include(s => s.Note).ThenInclude(n => n!.Ratings).ThenInclude(r => r.User)
            .FirstOrDefaultAsync(s => s.Token == token && s.IsActive);

        // Authenticity: expired or non-existent tokens yield the same 404 response
        if (shareLink?.Note is null) return NotFound();

        return View(shareLink.Note);
    }
}
