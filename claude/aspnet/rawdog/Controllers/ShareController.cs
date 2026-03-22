using LooseNotes.Data;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

public class ShareController : Controller
{
    private readonly ApplicationDbContext _db;

    public ShareController(ApplicationDbContext db)
    {
        _db = db;
    }

    // GET: Share/{token}
    [AllowAnonymous]
    public new async Task<IActionResult> View(string token)
    {
        var link = await _db.ShareLinks
            .Include(s => s.Note).ThenInclude(n => n.Owner)
            .Include(s => s.Note).ThenInclude(n => n.Attachments)
            .Include(s => s.Note).ThenInclude(n => n.Ratings).ThenInclude(r => r.User)
            .FirstOrDefaultAsync(s => s.Token == token && s.IsActive);

        if (link == null) return NotFound("This share link is invalid or has been revoked.");

        ViewData["SharedView"] = true;
        ViewData["ShareToken"] = token;
        return View(link.Note);
    }
}
