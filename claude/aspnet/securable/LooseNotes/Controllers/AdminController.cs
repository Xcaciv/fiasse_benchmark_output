// AdminController.cs — Administrative user and content management.
// Authorization: [Authorize(Roles = "Admin")] on the entire controller.
// Accountability: every admin action is audited with actor + target.
// Integrity: ownership changes verified against existing users in DB.
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.ViewModels.Admin;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

[Authorize(Roles = "Admin")]
public sealed class AdminController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _auditService;

    public AdminController(
        ApplicationDbContext db,
        UserManager<ApplicationUser> userManager,
        IAuditService auditService)
    {
        _db = db;
        _userManager = userManager;
        _auditService = auditService;
    }

    // ── GET /Admin ────────────────────────────────────────────────────────────
    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var recentAudit = await _db.AuditLogs
            .Include(a => a.Actor)
            .OrderByDescending(a => a.OccurredAt)
            .Take(20)
            .Select(a => new RecentAuditEntryViewModel
            {
                Action = a.Action,
                ActorUserName = a.Actor != null ? a.Actor.UserName : "deleted-user",
                ResourceType = a.ResourceType,
                ResourceId = a.ResourceId,
                OccurredAt = a.OccurredAt
            })
            .ToListAsync();

        var model = new AdminDashboardViewModel
        {
            TotalUsers = await _db.Users.CountAsync(),
            TotalNotes = await _db.Notes.CountAsync(),
            RecentAuditEntries = recentAudit
        };

        return View(model);
    }

    // ── GET /Admin/Users ──────────────────────────────────────────────────────
    [HttpGet]
    public async Task<IActionResult> Users(string? q)
    {
        var query = _db.Users
            .Include(u => u.Notes)
            .AsQueryable();

        if (!string.IsNullOrWhiteSpace(q))
        {
            // Integrity: EF Core parameterizes this automatically
            query = query.Where(u =>
                u.UserName!.Contains(q) || u.Email!.Contains(q));
        }

        var adminIds = (await _userManager.GetUsersInRoleAsync("Admin"))
            .Select(u => u.Id)
            .ToHashSet();

        var users = await query
            .OrderBy(u => u.UserName)
            .Select(u => new UserSummaryViewModel
            {
                Id = u.Id,
                UserName = u.UserName!,
                Email = u.Email!,
                CreatedAt = u.CreatedAt,
                NoteCount = u.Notes.Count,
                IsAdmin = adminIds.Contains(u.Id)
            })
            .ToListAsync();

        return View(new UserListViewModel
        {
            SearchQuery = q ?? string.Empty,
            Users = users
        });
    }

    // ── GET /Admin/ReassignNote/5 ─────────────────────────────────────────────
    [HttpGet]
    public async Task<IActionResult> ReassignNote(int id)
    {
        var note = await _db.Notes
            .Include(n => n.User)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note is null) return NotFound();

        var allUsers = await _db.Users
            .OrderBy(u => u.UserName)
            .Select(u => new UserOptionViewModel { Id = u.Id, UserName = u.UserName! })
            .ToListAsync();

        return View(new ReassignNoteViewModel
        {
            NoteId = note.Id,
            NoteTitle = note.Title,
            CurrentOwnerUserName = note.User?.UserName ?? string.Empty,
            AvailableUsers = allUsers
        });
    }

    // ── POST /Admin/ReassignNote ──────────────────────────────────────────────
    [HttpPost]
    public async Task<IActionResult> ReassignNote(ReassignNoteViewModel model)
    {
        if (!ModelState.IsValid)
            return await ReassignNote(model.NoteId);  // Reload with validation errors

        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note is null) return NotFound();

        // Integrity: verify target user exists before reassigning
        var targetUser = await _userManager.FindByIdAsync(model.TargetUserId);
        if (targetUser is null)
        {
            ModelState.AddModelError(nameof(model.TargetUserId), "Selected user does not exist.");
            return await ReassignNote(model.NoteId);
        }

        var previousOwnerId = note.UserId;
        note.UserId = model.TargetUserId;
        note.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();

        await _auditService.LogAsync(
            _userManager.GetUserId(User),
            "Admin.Note.Reassigned",
            "Note",
            note.Id.ToString(),
            $"From={previousOwnerId} To={model.TargetUserId}");

        return RedirectToAction(nameof(Index));
    }
}
