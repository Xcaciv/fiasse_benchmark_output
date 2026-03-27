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
public class AdminController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly IAuditService _audit;
    private readonly ILogger<AdminController> _logger;

    public AdminController(
        ApplicationDbContext db,
        UserManager<ApplicationUser> userManager,
        IAuditService audit,
        ILogger<AdminController> logger)
    {
        _db = db;
        _userManager = userManager;
        _audit = audit;
        _logger = logger;
    }

    private string GetAdminId() => _userManager.GetUserId(User)!;

    [HttpGet]
    public async Task<IActionResult> Dashboard()
    {
        var vm = new AdminDashboardViewModel
        {
            TotalUsers = await _userManager.Users.CountAsync(),
            TotalNotes = await _db.Notes.CountAsync(),
            RecentActivityLog = new List<string>
            {
                "Dashboard accessed by admin.",
                $"System has {await _db.Notes.CountAsync()} notes and {await _userManager.Users.CountAsync()} users."
            }
        };
        return View(vm);
    }

    [HttpGet]
    public async Task<IActionResult> UserList(string? q)
    {
        var query = _userManager.Users.AsQueryable();

        if (!string.IsNullOrWhiteSpace(q))
        {
            query = query.Where(u => u.UserName!.Contains(q) || u.Email!.Contains(q));
        }

        var users = await query.ToListAsync();

        var items = new List<UserListItemViewModel>();
        foreach (var u in users)
        {
            var noteCount = await _db.Notes.CountAsync(n => n.OwnerId == u.Id);
            items.Add(new UserListItemViewModel
            {
                Id = u.Id,
                UserName = u.UserName ?? string.Empty,
                Email = u.Email ?? string.Empty,
                RegistrationDate = u.CreatedAt,
                NoteCount = noteCount
            });
        }

        return View(new UserListViewModel { Users = items, SearchQuery = q });
    }

    [HttpGet]
    public async Task<IActionResult> ReassignNote(int noteId)
    {
        var note = await _db.Notes.FindAsync(noteId);
        if (note is null) return NotFound();

        var users = await _userManager.Users
            .Select(u => new UserSelectItem { Id = u.Id, DisplayText = u.UserName + " (" + u.Email + ")" })
            .ToListAsync();

        return View(new ReassignNoteViewModel
        {
            NoteId = noteId,
            NoteTitle = note.Title,
            Users = users
        });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote([Bind("NoteId,NewOwnerId")] ReassignNoteViewModel model)
    {
        if (!ModelState.IsValid)
        {
            return await ReassignNote(model.NoteId);
        }

        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note is null) return NotFound();

        // Verify new owner exists — Derived Integrity Principle
        var newOwner = await _userManager.FindByIdAsync(model.NewOwnerId);
        if (newOwner is null)
        {
            ModelState.AddModelError("NewOwnerId", "User not found.");
            return await ReassignNote(model.NoteId);
        }

        var oldOwnerId = note.OwnerId;
        note.OwnerId = model.NewOwnerId;
        note.UpdatedAt = DateTime.UtcNow;

        await _db.SaveChangesAsync();
        _audit.LogAdminAction("REASSIGN_NOTE", GetAdminId(),
            $"NoteId={model.NoteId} from OwnerId={oldOwnerId} to OwnerId={model.NewOwnerId}");

        TempData["Success"] = "Note reassigned.";
        return RedirectToAction(nameof(UserList));
    }
}
