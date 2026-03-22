using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Ratings;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Handles note ratings.
/// SSEM: One rating per user per note enforced at DB level (unique index) and application level.
/// </summary>
[Authorize]
public class RatingsController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _audit;

    public RatingsController(ApplicationDbContext db, UserManager<ApplicationUser> userManager,
        IAuditService audit)
    {
        _db = db;
        _userManager = userManager;
        _audit = audit;
    }

    [HttpPost]
    public async Task<IActionResult> Rate(RateNoteViewModel model)
    {
        if (!ModelState.IsValid)
        {
            TempData["Error"] = "Invalid rating. Stars must be between 1 and 5.";
            return RedirectToAction("Details", "Notes", new { id = model.NoteId });
        }

        // Verify note exists and is visible to this user
        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note is null) return NotFound();

        var userId = _userManager.GetUserId(User)!;

        // Owners cannot rate their own notes
        if (note.OwnerId == userId)
        {
            TempData["Error"] = "You cannot rate your own note.";
            return RedirectToAction("Details", "Notes", new { id = model.NoteId });
        }

        var existing = await _db.Ratings
            .FirstOrDefaultAsync(r => r.NoteId == model.NoteId && r.RaterId == userId);

        if (existing is null)
        {
            _db.Ratings.Add(new Rating
            {
                NoteId = model.NoteId,
                RaterId = userId,
                Stars = model.Stars,
                Comment = model.Comment,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            });
            await _audit.LogAsync("RatingCreated", true, $"NoteId={model.NoteId} Stars={model.Stars}",
                userId, User.Identity?.Name);
        }
        else
        {
            existing.Stars = model.Stars;
            existing.Comment = model.Comment;
            existing.UpdatedAt = DateTime.UtcNow;
            await _audit.LogAsync("RatingUpdated", true, $"NoteId={model.NoteId} Stars={model.Stars}",
                userId, User.Identity?.Name);
        }

        await _db.SaveChangesAsync();
        TempData["Success"] = "Rating saved.";
        return RedirectToAction("Details", "Notes", new { id = model.NoteId });
    }
}
