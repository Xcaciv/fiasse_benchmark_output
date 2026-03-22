using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Notes;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Handles note ratings: submit and edit.
/// One rating per user per note enforced at DB (unique constraint) and service level.
/// RaterId set server-side — never client-supplied (Derived Integrity).
/// </summary>
[Authorize]
public class RatingsController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _auditService;

    public RatingsController(
        ApplicationDbContext db,
        UserManager<ApplicationUser> userManager,
        IAuditService auditService)
    {
        _db = db;
        _userManager = userManager;
        _auditService = auditService;
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> Submit(RatingInputViewModel model)
    {
        if (!ModelState.IsValid)
            return RedirectToAction("Details", "Notes", new { id = model.NoteId });

        var userId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(model.NoteId);

        if (note is null) return NotFound();
        // Owners cannot rate their own notes (Integrity)
        if (note.OwnerId == userId)
        {
            TempData["Error"] = "You cannot rate your own note.";
            return RedirectToAction("Details", "Notes", new { id = model.NoteId });
        }

        // Check for existing rating (upsert logic)
        var existing = await _db.Ratings
            .FirstOrDefaultAsync(r => r.NoteId == model.NoteId && r.RaterId == userId);

        if (existing is not null)
        {
            // Update existing rating — value validated by model annotations
            existing.Value = model.Value;
            existing.Comment = model.Comment;
            existing.UpdatedAt = DateTime.UtcNow;
            await _db.SaveChangesAsync();

            await _auditService.RecordAsync("RatingUpdated", userId: userId,
                resourceType: "Rating", resourceId: existing.Id.ToString());
        }
        else
        {
            var rating = new Rating
            {
                NoteId = model.NoteId,
                // RaterId set from server identity — never from form (Derived Integrity)
                RaterId = userId,
                Value = model.Value,
                Comment = model.Comment,
                CreatedAt = DateTime.UtcNow,
                UpdatedAt = DateTime.UtcNow
            };
            _db.Ratings.Add(rating);
            await _db.SaveChangesAsync();

            await _auditService.RecordAsync("RatingSubmitted", userId: userId,
                resourceType: "Rating", resourceId: rating.Id.ToString());
        }

        return RedirectToAction("Details", "Notes", new { id = model.NoteId });
    }
}
