using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services.Interfaces;
using LooseNotes.ViewModels.Admin;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Controllers;

/// <summary>
/// Admin-only actions. All routes protected with Role="Admin" (Authenticity).
/// Admin actions are fully audited (Accountability).
/// </summary>
[Authorize(Roles = "Admin")]
[Route("[controller]/[action]")]
public class AdminController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _auditService;
    private readonly ILogger<AdminController> _logger;

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

    [HttpGet("/Admin")]
    public async Task<IActionResult> Index()
    {
        _logger.LogInformation("Admin/Index accessed by {UserId}", _userManager.GetUserId(User));

        var userCount = await _db.Users.CountAsync();
        var noteCount = await _db.Notes.CountAsync();
        var recentActivity = await _db.ActivityLogs
            .AsNoTracking()
            .OrderByDescending(a => a.Timestamp)
            .Take(50)
            .ToListAsync();

        return View(new AdminDashboardViewModel
        {
            TotalUsers = userCount,
            TotalNotes = noteCount,
            RecentActivity = recentActivity
        });
    }

    [HttpGet]
    public async Task<IActionResult> Users(string? q)
    {
        _logger.LogInformation("Admin/Users accessed by {UserId}", _userManager.GetUserId(User));

        IQueryable<ApplicationUser> query = _db.Users.AsNoTracking();

        if (!string.IsNullOrWhiteSpace(q))
        {
            var lower = q.ToLower();
            query = query.Where(u =>
                u.UserName!.ToLower().Contains(lower) ||
                u.Email!.ToLower().Contains(lower));
        }

        var users = await query.OrderBy(u => u.UserName).ToListAsync();
        return View(new UserListViewModel { Users = users, SearchQuery = q });
    }

    [HttpGet]
    public async Task<IActionResult> ReassignNote(int noteId)
    {
        var note = await _db.Notes.AsNoTracking()
            .Include(n => n.User)
            .FirstOrDefaultAsync(n => n.Id == noteId);

        if (note is null) return NotFound();

        var users = await _db.Users.AsNoTracking()
            .OrderBy(u => u.UserName)
            .ToListAsync();

        return View(new ReassignNoteViewModel
        {
            NoteId = noteId,
            NoteTitle = note.Title,
            CurrentOwnerName = note.User?.UserName ?? "(unknown)",
            AvailableUsers = users
        });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote(ReassignNoteViewModel model)
    {
        // Trust boundary: validate that target user exists before reassignment (Integrity)
        _logger.LogInformation("Admin reassigning note {NoteId} to user {NewOwner}", model.NoteId, model.NewOwnerId);

        if (!ModelState.IsValid) return await ReassignNote(model.NoteId);

        var note = await _db.Notes.FirstOrDefaultAsync(n => n.Id == model.NoteId);
        if (note is null) return NotFound();

        var newOwner = await _userManager.FindByIdAsync(model.NewOwnerId);
        if (newOwner is null)
        {
            ModelState.AddModelError(nameof(model.NewOwnerId), "User not found.");
            return await ReassignNote(model.NoteId);
        }

        var adminId = _userManager.GetUserId(User)!;
        var oldOwnerId = note.UserId;
        note.UserId = model.NewOwnerId;
        await _db.SaveChangesAsync();

        await _auditService.LogAsync("NoteReassigned", adminId,
            $"NoteId={model.NoteId} From={oldOwnerId} To={model.NewOwnerId}", GetClientIp());

        TempData["SuccessMessage"] = "Note reassigned successfully.";
        return RedirectToAction(nameof(Index));
    }

    private string? GetClientIp() =>
        HttpContext.Connection.RemoteIpAddress?.ToString();
}
