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
/// Admin-only controller. Class-level [Authorize(Roles="Admin")] ensures all actions
/// are protected by a single control - no per-action gaps possible (FIASSE S2.4, ASVS V8.1.1).
/// All admin actions produce audit log entries (Accountability, ASVS V16.2.1).
/// </summary>
[Authorize(Roles = "Admin")]
[AutoValidateAntiforgeryToken]
public sealed class AdminController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _auditService;
    private readonly ILogger<AdminController> _logger;
    private const int PageSize = 20;

    public AdminController(
        ApplicationDbContext db,
        UserManager<ApplicationUser> userManager,
        IAuditService auditService,
        ILogger<AdminController> logger)
    {
        _db = db;
        _userManager = userManager;
        _auditService = auditService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<IActionResult> Dashboard(int page = 1)
    {
        var adminId = _userManager.GetUserId(User)!;

        var totalUsers = await _db.Users.CountAsync();
        var totalNotes = await _db.Notes.CountAsync();
        var totalPublicNotes = await _db.Notes.CountAsync(n => n.IsPublic);
        var totalAttachments = await _db.Attachments.CountAsync();

        var totalLogs = await _db.AuditLogs.CountAsync();
        var totalPages = (int)Math.Ceiling(totalLogs / (double)PageSize);
        var safePage = Math.Clamp(page, 1, Math.Max(1, totalPages));

        var recentLogs = await _db.AuditLogs
            .OrderByDescending(l => l.Timestamp)
            .Skip((safePage - 1) * PageSize)
            .Take(PageSize)
            .Select(l => new RecentAuditLogViewModel
            {
                Id = l.Id,
                Timestamp = l.Timestamp,
                Username = l.Username,
                EventType = l.EventType,
                ResourceType = l.ResourceType,
                ResourceId = l.ResourceId,
                Outcome = l.Outcome,
                SourceIp = l.SourceIp
                // Details intentionally omitted from list view
            })
            .ToListAsync();

        await _auditService.LogAsync(
            AuditEventTypes.AdminDashboardViewed,
            adminId, User.Identity?.Name, GetClientIp(),
            resourceType: "admin", resourceId: "dashboard");

        return View(new AdminDashboardViewModel
        {
            TotalUsers = totalUsers,
            TotalNotes = totalNotes,
            TotalPublicNotes = totalPublicNotes,
            TotalAttachments = totalAttachments,
            RecentAuditLogs = recentLogs,
            CurrentPage = safePage,
            TotalPages = totalPages
        });
    }

    [HttpGet]
    public async Task<IActionResult> Users(string? search, int page = 1)
    {
        var adminId = _userManager.GetUserId(User)!;

        var query = _db.Users.AsQueryable();

        if (!string.IsNullOrWhiteSpace(search))
        {
            var normalizedSearch = search.ToLower();
            query = query.Where(u =>
                u.UserName!.ToLower().Contains(normalizedSearch) ||
                u.Email!.ToLower().Contains(normalizedSearch));
        }

        var totalCount = await query.CountAsync();
        var totalPages = (int)Math.Ceiling(totalCount / (double)PageSize);
        var safePage = Math.Clamp(page, 1, Math.Max(1, totalPages));

        var users = await query
            .OrderBy(u => u.UserName)
            .Skip((safePage - 1) * PageSize)
            .Take(PageSize)
            .ToListAsync();

        var userSummaries = new List<UserSummaryViewModel>();
        foreach (var u in users)
        {
            var noteCount = await _db.Notes.CountAsync(n => n.UserId == u.Id);
            var isAdmin = await _userManager.IsInRoleAsync(u, "Admin");
            userSummaries.Add(new UserSummaryViewModel
            {
                Id = u.Id,
                Username = u.UserName ?? string.Empty,
                Email = u.Email ?? string.Empty,
                CreatedAt = u.CreatedAt,
                LastLoginAt = u.LastLoginAt,
                NoteCount = noteCount,
                IsAdmin = isAdmin
                // Password hash, tokens, security stamp excluded (Confidentiality)
            });
        }

        await _auditService.LogAsync(
            string.IsNullOrWhiteSpace(search)
                ? AuditEventTypes.AdminUserViewed
                : AuditEventTypes.AdminUserSearched,
            adminId, User.Identity?.Name, GetClientIp(),
            resourceType: "admin", resourceId: "users",
            details: string.IsNullOrWhiteSpace(search) ? null : $"query_length:{search.Length}");

        return View(new UserListViewModel
        {
            Users = userSummaries,
            SearchQuery = search,
            CurrentPage = safePage,
            TotalPages = totalPages,
            TotalCount = totalCount
        });
    }

    [HttpGet]
    public async Task<IActionResult> ReassignNote(int noteId)
    {
        var note = await _db.Notes.Include(n => n.User).FirstOrDefaultAsync(n => n.Id == noteId);
        if (note == null) return NotFound();

        return View(new ReassignNoteViewModel
        {
            NoteId = note.Id,
            CurrentTitle = note.Title,
            CurrentOwnerUsername = note.User.UserName ?? string.Empty
        });
    }

    // GET is explicitly rejected - reassignment only via POST with anti-forgery token (ASVS V3.5)
    [HttpPost]
    public async Task<IActionResult> ReassignNote(ReassignNoteViewModel model)
    {
        if (!ModelState.IsValid)
        {
            var noteForDisplay = await _db.Notes.Include(n => n.User).FirstOrDefaultAsync(n => n.Id == model.NoteId);
            if (noteForDisplay != null)
            {
                model.CurrentTitle = noteForDisplay.Title;
                model.CurrentOwnerUsername = noteForDisplay.User.UserName ?? string.Empty;
            }
            return View(model);
        }

        var adminId = _userManager.GetUserId(User)!;
        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note == null) return NotFound();

        // Validate target user exists server-side (ASVS V8.2.1)
        var targetUser = await _userManager.FindByNameAsync(model.TargetUsername);
        if (targetUser == null)
        {
            ModelState.AddModelError(nameof(model.TargetUsername), "User not found.");
            var noteForDisplay = await _db.Notes.Include(n => n.User).FirstOrDefaultAsync(n => n.Id == model.NoteId);
            if (noteForDisplay != null)
            {
                model.CurrentTitle = noteForDisplay.Title;
                model.CurrentOwnerUsername = noteForDisplay.User.UserName ?? string.Empty;
            }
            return View(model);
        }

        if (targetUser.Id == note.UserId)
        {
            ModelState.AddModelError(string.Empty, "Note is already owned by this user.");
            return View(model);
        }

        var previousOwnerId = note.UserId;
        note.UserId = targetUser.Id;
        note.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();

        // Detailed audit log: all actors, note ID, both owners (ASVS V16.2.1, FIASSE S2.6)
        await _auditService.LogAsync(
            AuditEventTypes.NoteOwnerReassigned,
            adminId, User.Identity?.Name, GetClientIp(),
            outcome: "success",
            resourceType: "note", resourceId: note.Id.ToString(),
            details: $"prev_owner:{previousOwnerId} new_owner:{targetUser.Id}");

        _logger.LogInformation(
            "Admin {AdminId} reassigned note {NoteId} from {PrevOwner} to {NewOwner}",
            adminId, note.Id, previousOwnerId, targetUser.Id);

        TempData["Success"] = $"Note successfully reassigned to {model.TargetUsername}.";
        return RedirectToAction(nameof(Dashboard));
    }

    [HttpPost]
    public async Task<IActionResult> TerminateUserSessions(string userId)
    {
        var adminId = _userManager.GetUserId(User)!;
        var targetUser = await _userManager.FindByIdAsync(userId);
        if (targetUser == null) return NotFound();

        // Invalidate all sessions by updating security stamp (ASVS V7.4.5)
        await _userManager.UpdateSecurityStampAsync(targetUser);

        await _auditService.LogAsync(
            "admin.sessions.terminated",
            adminId, User.Identity?.Name, GetClientIp(),
            resourceType: "user", resourceId: userId);

        TempData["Success"] = $"Sessions terminated for user {targetUser.UserName}.";
        return RedirectToAction(nameof(Users));
    }

    private string GetClientIp()
        => HttpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown";
}
