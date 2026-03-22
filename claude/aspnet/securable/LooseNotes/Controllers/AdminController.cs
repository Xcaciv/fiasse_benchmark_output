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
/// Admin-only operations: dashboard, user listing, note reassignment.
/// All actions require the Admin role (Authenticity, Authorization).
/// All actions are audited (Accountability).
/// </summary>
[Authorize(Roles = "Admin")]
public class AdminController : Controller
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

    // ── Dashboard ─────────────────────────────────────────────────────────────

    [HttpGet]
    public async Task<IActionResult> Dashboard()
    {
        var totalUsers = await _db.Users.CountAsync();
        var totalNotes = await _db.Notes.CountAsync();
        var recentActivity = await _db.AuditLogs
            .OrderByDescending(a => a.OccurredAt)
            .Take(20) // Availability: bound result
            .Select(a => new RecentAuditItem
            {
                Action = a.Action,
                UserId = a.UserId,
                ResourceType = a.ResourceType,
                ResourceId = a.ResourceId,
                Succeeded = a.Succeeded,
                OccurredAt = a.OccurredAt
            })
            .ToListAsync();

        var vm = new AdminDashboardViewModel
        {
            TotalUsers = totalUsers,
            TotalNotes = totalNotes,
            RecentActivity = recentActivity
        };
        return View(vm);
    }

    // ── User List ─────────────────────────────────────────────────────────────

    [HttpGet]
    public async Task<IActionResult> Users(UserListViewModel model)
    {
        var query = _db.Users.AsQueryable();

        if (!string.IsNullOrWhiteSpace(model.SearchQuery))
        {
            var search = model.SearchQuery.Trim();
            query = query.Where(u =>
                u.UserName!.Contains(search) || u.Email!.Contains(search));
        }

        var users = await query
            .OrderBy(u => u.Email)
            .Take(200) // Availability: bound result
            .Select(u => new UserListItem
            {
                Id = u.Id,
                DisplayName = u.DisplayName,
                Email = u.Email ?? string.Empty,
                CreatedAt = u.CreatedAt,
                NoteCount = u.Notes.Count
            })
            .ToListAsync();

        // Load roles separately (Identity API — not EF navigable as query)
        foreach (var item in users)
            item.Roles = (await _userManager.GetRolesAsync(
                (await _userManager.FindByIdAsync(item.Id))!)).ToList();

        model.Users = users;
        return View(model);
    }

    // ── Note Reassignment ─────────────────────────────────────────────────────

    [HttpGet]
    public async Task<IActionResult> ReassignNote(int id)
    {
        var note = await _db.Notes.Include(n => n.Owner).FirstOrDefaultAsync(n => n.Id == id);
        if (note is null) return NotFound();

        var allEmails = await _db.Users
            .OrderBy(u => u.Email)
            .Select(u => u.Email!)
            .ToListAsync();

        var vm = new ReassignNoteViewModel
        {
            NoteId = note.Id,
            NoteTitle = note.Title,
            CurrentOwnerDisplayName = note.Owner?.DisplayName ?? string.Empty,
            AvailableUserEmails = allEmails
        };
        return View(vm);
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote(ReassignNoteViewModel model)
    {
        if (!ModelState.IsValid)
            return await ReassignNote(model.NoteId);

        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note is null) return NotFound();

        var newOwner = await _userManager.FindByEmailAsync(model.NewOwnerEmail);
        if (newOwner is null)
        {
            ModelState.AddModelError(nameof(model.NewOwnerEmail), "User not found.");
            return await ReassignNote(model.NoteId);
        }

        var adminId = _userManager.GetUserId(User)!;
        var previousOwnerId = note.OwnerId;

        note.OwnerId = newOwner.Id;
        note.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();

        await _auditService.RecordAsync("NoteReassigned", userId: adminId,
            resourceType: "Note", resourceId: note.Id.ToString(),
            metadataJson: $"{{\"from\":\"{previousOwnerId}\",\"to\":\"{newOwner.Id}\"}}");

        TempData["Success"] = $"Note reassigned to {model.NewOwnerEmail}.";
        return RedirectToAction(nameof(Dashboard));
    }
}
