using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;

namespace LooseNotes.Controllers;

public class ShareController : Controller
{
    private readonly ApplicationDbContext _db;

    public ShareController(ApplicationDbContext db)
    {
        _db = db;
    }

    [HttpGet]
    [ActionName("View")]
    public async Task<IActionResult> ViewNote(string token)
    {
        if (string.IsNullOrWhiteSpace(token)) return NotFound();

        var shareLink = await _db.ShareLinks
            .Include(s => s.Note)
                .ThenInclude(n => n!.User)
            .Include(s => s.Note)
                .ThenInclude(n => n!.Attachments)
            .Include(s => s.Note)
                .ThenInclude(n => n!.Ratings)
                    .ThenInclude(r => r.User)
            .FirstOrDefaultAsync(s => s.Token == token);

        if (shareLink?.Note == null) return NotFound();

        return View(shareLink.Note);
    }
}
