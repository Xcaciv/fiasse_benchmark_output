using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;

namespace LooseNotes.Controllers;

[Authorize(Roles = "Admin")]
public class AdminController : Controller
{
    private readonly ApplicationDbContext _context;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IActivityLogService _activityLog;

    public AdminController(
        ApplicationDbContext context,
        UserManager<ApplicationUser> userManager,
        IActivityLogService activityLog)
    {
        _context = context;
        _userManager = userManager;
        _activityLog = activityLog;
    }

    public async Task<IActionResult> Index()
    {
        var userCount = await _context.Users.CountAsync();
        var noteCount = await _context.Notes.CountAsync();
        var ratingCount = await _context.Ratings.CountAsync();
        var attachmentCount = await _context.Attachments.CountAsync();

        var recentLogs = await _context.ActivityLogs
            .OrderByDescending(l => l.CreatedAt)
            .Take(50)
            .ToListAsync();

        var model = new AdminDashboardViewModel
        {
            TotalUsers = userCount,
            TotalNotes = noteCount,
            TotalRatings = ratingCount,
            TotalAttachments = attachmentCount,
            RecentActivity = recentLogs
        };

        return View(model);
    }

    public async Task<IActionResult> Users(string? search)
    {
        var query = _context.Users.AsQueryable();

        if (!string.IsNullOrWhiteSpace(search))
        {
            search = search.ToLower();
            query = query.Where(u => 
                u.UserName!.ToLower().Contains(search) || 
                u.Email!.ToLower().Contains(search));
        }

        var users = await query
            .Select(u => new UserListViewModel
            {
                Id = u.Id,
                UserName = u.UserName ?? string.Empty,
                Email = u.Email ?? string.Empty,
                DisplayName = u.DisplayName,
                CreatedAt = u.CreatedAt,
                LastLoginAt = u.LastLoginAt,
                NoteCount = u.Notes.Count,
                Role = _context.UserRoles.Where(ur => ur.UserId == u.Id)
                    .Select(ur => _context.Roles.Where(r => r.Id == ur.RoleId).Select(r => r.Name).FirstOrDefault())
                    .FirstOrDefault() ?? "User"
            })
            .OrderByDescending(u => u.CreatedAt)
            .ToListAsync();

        ViewBag.Search = search;
        return View(users);
    }

    public async Task<IActionResult> ReassignNote(int id)
    {
        var note = await _context.Notes
            .Include(n => n.User)
            .FirstOrDefaultAsync(n => n.Id == id);

        if (note == null)
            return NotFound();

        var users = await _context.Users
            .Select(u => new { u.Id, u.UserName })
            .ToListAsync();

        ViewBag.Note = note;
        ViewBag.Users = users;
        return View();
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote(int noteId, string newOwnerId)
    {
        var note = await _context.Notes.FindAsync(noteId);
        if (note == null)
            return NotFound();

        var newOwner = await _userManager.FindByIdAsync(newOwnerId);
        if (newOwner == null)
        {
            ModelState.AddModelError(string.Empty, "Selected user not found.");
            return RedirectToAction("ReassignNote", new { id = noteId });
        }

        var oldOwnerId = note.UserId;
        var adminId = _userManager.GetUserId(User);

        note.UserId = newOwnerId;
        note.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        await _activityLog.LogAsync("Note Reassigned", adminId, 
            $"Note '{note.Title}' reassigned from user {oldOwnerId} to {newOwnerId}", "Note", noteId);

        TempData["SuccessMessage"] = $"Note '{note.Title}' has been reassigned to {newOwner.UserName}.";
        return RedirectToAction("Index");
    }

    public async Task<IActionResult> ActivityLogs()
    {
        var logs = await _context.ActivityLogs
            .OrderByDescending(l => l.CreatedAt)
            .Take(100)
            .ToListAsync();

        return View(logs);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> AddAdmin(string userId)
    {
        var user = await _userManager.FindByIdAsync(userId);
        if (user == null)
            return NotFound();

        await _userManager.AddToRoleAsync(user, "Admin");
        
        var adminId = _userManager.GetUserId(User);
        await _activityLog.LogAsync("Admin Added", adminId, $"User {user.UserName} was granted admin role", "User", userId);

        TempData["SuccessMessage"] = $"{user.UserName} has been granted admin privileges.";
        return RedirectToAction("Users");
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> RemoveAdmin(string userId)
    {
        var user = await _userManager.FindByIdAsync(userId);
        if (user == null)
            return NotFound();

        await _userManager.RemoveFromRoleAsync(user, "Admin");
        
        var adminId = _userManager.GetUserId(User);
        await _activityLog.LogAsync("Admin Removed", adminId, $"Admin role removed from {user.UserName}", "User", userId);

        TempData["SuccessMessage"] = $"Admin privileges have been revoked from {user.UserName}.";
        return RedirectToAction("Users");
    }
}

public class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public int TotalRatings { get; set; }
    public int TotalAttachments { get; set; }
    public List<ActivityLog> RecentActivity { get; set; } = new();
}

public class UserListViewModel
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string DisplayName { get; set; } = string.Empty;
    public DateTime CreatedAt { get; set; }
    public DateTime? LastLoginAt { get; set; }
    public int NoteCount { get; set; }
    public string Role { get; set; } = "User";
}
