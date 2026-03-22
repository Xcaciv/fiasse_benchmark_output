using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services.Interfaces;
using LooseNotes.ViewModels.Notes;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Manages note ratings. One rating per user per note enforced by DB index (Integrity).
/// </summary>
[Authorize]
[Route("[controller]/[action]")]
public class RatingsController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _auditService;
    private readonly ILogger<RatingsController> _logger;

    public RatingsController(
        ApplicationDbContext db,
        UserManager<ApplicationUser> userManager,
        IAuditService auditService,
        ILogger<RatingsController> logger)
    {
        _db = db;
        _userManager = userManager;
        _auditService = auditService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<IActionResult> RateNote(int noteId)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.AsNoTracking().FirstOrDefaultAsync(n => n.Id == noteId);
        if (note is null) return NotFound();
        if (note.UserId == userId) return BadRequest("You cannot rate your own note.");

        var existing = await _db.Ratings.AsNoTracking()
            .FirstOrDefaultAsync(r => r.NoteId == noteId && r.UserId == userId);

        var vm = new RateNoteViewModel
        {
            NoteId = noteId,
            NoteTitle = note.Title,
            Stars = existing?.Stars ?? 0,
            Comment = existing?.Comment,
            ExistingRatingId = existing?.Id
        };
        return View(vm);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RateNote(RateNoteViewModel model)
    {
        // Trust boundary: validate all rating fields (Integrity)
        _logger.LogInformation("Rating submitted for note {NoteId} by {UserId}", model.NoteId, _userManager.GetUserId(User));

        if (!ModelState.IsValid) return View(model);

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.AsNoTracking().FirstOrDefaultAsync(n => n.Id == model.NoteId);
        if (note is null) return NotFound();
        if (note.UserId == userId) return BadRequest("You cannot rate your own note.");

        await UpsertRating(model.NoteId, userId, model.Stars, model.Comment);
        await _auditService.LogAsync("NoteRated", userId, $"NoteId={model.NoteId} Stars={model.Stars}", GetClientIp());

        return RedirectToAction("Details", "Notes", new { id = model.NoteId });
    }

    [HttpGet]
    public async Task<IActionResult> Edit(int id)
    {
        var userId = _userManager.GetUserId(User)!;
        var rating = await _db.Ratings.AsNoTracking()
            .Include(r => r.Note)
            .FirstOrDefaultAsync(r => r.Id == id && r.UserId == userId);

        if (rating is null) return NotFound();

        var vm = new RateNoteViewModel
        {
            NoteId = rating.NoteId,
            NoteTitle = rating.Note?.Title ?? string.Empty,
            Stars = rating.Stars,
            Comment = rating.Comment,
            ExistingRatingId = rating.Id
        };
        return View("RateNote", vm);
    }

    [HttpGet]
    public async Task<IActionResult> NoteRatings(int noteId)
    {
        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.AsNoTracking().FirstOrDefaultAsync(n => n.Id == noteId && n.UserId == userId);
        if (note is null) return Forbid();

        var ratings = await _db.Ratings.AsNoTracking()
            .Include(r => r.User)
            .Where(r => r.NoteId == noteId)
            .OrderByDescending(r => r.CreatedAt)
            .ToListAsync();

        ViewBag.NoteTitle = note.Title;
        ViewBag.NoteId = noteId;
        return View(ratings);
    }

    private async Task UpsertRating(int noteId, string userId, int stars, string? comment)
    {
        var existing = await _db.Ratings.FirstOrDefaultAsync(r => r.NoteId == noteId && r.UserId == userId);

        if (existing is null)
        {
            _db.Ratings.Add(new Rating
            {
                NoteId = noteId,
                UserId = userId,
                Stars = stars,
                Comment = comment,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
        }
        else
        {
            existing.Stars = stars;
            existing.Comment = comment;
            existing.UpdatedAt = DateTime.UtcNow;
        }

        await _db.SaveChangesAsync();
    }

    private string? GetClientIp() =>
        HttpContext.Connection.RemoteIpAddress?.ToString();
}
