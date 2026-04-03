using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;

namespace LooseNotes.Controllers;

public class ShareController : Controller
{
    private readonly ApplicationDbContext _context;

    public ShareController(ApplicationDbContext context)
    {
        _context = context;
    }

    // GET: /Share/View - no authentication required (§10)
    [HttpGet]
    [ActionName("View")]
    public async Task<IActionResult> SharedView(string token)
    {
        var shareLink = await _context.ShareLinks
            .Include(s => s.Note)
                .ThenInclude(n => n!.Ratings)
            .Include(s => s.Note)
                .ThenInclude(n => n!.Attachments)
            .FirstOrDefaultAsync(s => s.Token == token);

        if (shareLink == null || shareLink.Note == null)
            return NotFound();

        return View(shareLink.Note);
    }
}
