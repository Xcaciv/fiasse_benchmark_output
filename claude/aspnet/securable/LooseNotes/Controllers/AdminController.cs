using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Admin;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Admin-only dashboard: user management, note reassignment, activity log.
/// FIASSE: All actions require Admin role – enforced at controller level.
/// </summary>
[Authorize(Roles = "Admin")]
public class AdminController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _audit;

    public AdminController(ApplicationDbContext db, UserManager<ApplicationUser> userManager,
        IAuditService audit)
    {
        _db = db;
        _userManager = userManager;
        _audit = audit;
    }

    // -----------------------------------------------------------------------
    // Dashboard
    // -----------------------------------------------------------------------
    [HttpGet]
    public async Task<IActionResult> Dashboard()
    {
        var vm = new DashboardViewModel
        {
            TotalUsers = await _userManager.Users.CountAsync(),
            TotalNotes = await _db.Notes.CountAsync(),
            RecentActivity = await _db.AuditLogs
                .OrderByDescending(a => a.OccurredAt)
                .Take(50)
                .ToListAsync()
        };
        return View(vm);
    }

    // -----------------------------------------------------------------------
    // User list
    // -----------------------------------------------------------------------
    [HttpGet]
    public async Task<IActionResult> Users(string? q)
    {
        var query = _userManager.Users.AsQueryable();
        if (!string.IsNullOrWhiteSpace(q))
        {
            var lower = q.ToLower();
            query = query.Where(u =>
                u.UserName!.ToLower().Contains(lower) ||
                u.Email!.ToLower().Contains(lower));
        }

        var users = await query.ToListAsync();
        var summaries = new List<UserSummary>();

        foreach (var u in users)
        {
            var noteCount = await _db.Notes.CountAsync(n => n.OwnerId == u.Id);
            var isAdmin = await _userManager.IsInRoleAsync(u, DbInitializer.AdminRole);
            summaries.Add(new UserSummary
            {
                Id = u.Id,
                UserName = u.UserName ?? string.Empty,
                Email = u.Email ?? string.Empty,
                CreatedAt = u.CreatedAt,
                NoteCount = noteCount,
                IsAdmin = isAdmin
            });
        }

        return View(new UserListViewModel { SearchQuery = q, Users = summaries });
    }

    // -----------------------------------------------------------------------
    // Note reassignment
    // -----------------------------------------------------------------------
    [HttpGet]
    public async Task<IActionResult> ReassignNote(int noteId)
    {
        var note = await _db.Notes.Include(n => n.Owner).FirstOrDefaultAsync(n => n.Id == noteId);
        if (note is null) return NotFound();

        var allUsers = await _userManager.Users
            .Select(u => new UserOption { Id = u.Id, UserName = u.UserName ?? string.Empty })
            .ToListAsync();

        return View(new ReassignNoteViewModel
        {
            NoteId = note.Id,
            NoteTitle = note.Title,
            CurrentOwnerId = note.OwnerId,
            CurrentOwnerName = note.Owner.UserName ?? string.Empty,
            AllUsers = allUsers
        });
    }

    [HttpPost]
    public async Task<IActionResult> ReassignNote(ReassignNoteViewModel model)
    {
        if (!ModelState.IsValid)
        {
            // Re-populate user list
            model.AllUsers = await _userManager.Users
                .Select(u => new UserOption { Id = u.Id, UserName = u.UserName ?? string.Empty })
                .ToListAsync();
            return View(model);
        }

        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note is null) return NotFound();

        // Verify new owner exists
        var newOwner = await _userManager.FindByIdAsync(model.NewOwnerId);
        if (newOwner is null)
        {
            ModelState.AddModelError(nameof(model.NewOwnerId), "Selected user not found.");
            model.AllUsers = await _userManager.Users
                .Select(u => new UserOption { Id = u.Id, UserName = u.UserName ?? string.Empty })
                .ToListAsync();
            return View(model);
        }

        var previousOwnerId = note.OwnerId;
        note.OwnerId = model.NewOwnerId;
        note.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();

        var adminId = _userManager.GetUserId(User);
        await _audit.LogAsync("NoteReassigned", true,
            $"NoteId={note.Id} From={previousOwnerId} To={model.NewOwnerId}",
            adminId, User.Identity?.Name);

        TempData["Success"] = $"Note reassigned to {newOwner.UserName}.";
        return RedirectToAction(nameof(Dashboard));
    }
}
