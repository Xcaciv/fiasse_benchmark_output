using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.ViewModels;

namespace LooseNotes.Controllers;

[Authorize(Roles = "Admin")]
public class AdminController : Controller
{
    private readonly ApplicationDbContext _db;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly ILogger<AdminController> _logger;

    public AdminController(
        ApplicationDbContext db,
        UserManager<ApplicationUser> userManager,
        ILogger<AdminController> logger)
    {
        _db = db;
        _userManager = userManager;
        _logger = logger;
    }

    public async Task<IActionResult> Index()
    {
        var vm = new AdminDashboardViewModel
        {
            TotalUsers = await _db.Users.CountAsync(),
            TotalNotes = await _db.Notes.CountAsync(),
            RecentActivity = new List<ActivityLogEntry>()
        };
        return View(vm);
    }

    [HttpGet]
    public async Task<IActionResult> Users(string? q)
    {
        var query = _db.Users.AsQueryable();

        if (!string.IsNullOrWhiteSpace(q))
        {
            var lowerQ = q.ToLower();
            query = query.Where(u => u.UserName!.ToLower().Contains(lowerQ) || u.Email!.ToLower().Contains(lowerQ));
        }

        var users = await query.ToListAsync();
        var items = new List<AdminUserItemViewModel>();

        foreach (var user in users)
        {
            var noteCount = await _db.Notes.CountAsync(n => n.UserId == user.Id);
            var isAdmin = await _userManager.IsInRoleAsync(user, "Admin");
            items.Add(new AdminUserItemViewModel
            {
                Id = user.Id,
                Username = user.UserName ?? string.Empty,
                Email = user.Email ?? string.Empty,
                CreatedAt = user.CreatedAt,
                NoteCount = noteCount,
                IsAdmin = isAdmin
            });
        }

        return View(new AdminUsersViewModel { SearchQuery = q, Users = items });
    }

    [HttpGet]
    public async Task<IActionResult> ReassignNote(int noteId)
    {
        var note = await _db.Notes.Include(n => n.User).FirstOrDefaultAsync(n => n.Id == noteId);
        if (note == null) return NotFound();

        var allUsers = await _db.Users.ToListAsync();
        var users = new List<AdminUserItemViewModel>();
        foreach (var u in allUsers)
        {
            users.Add(new AdminUserItemViewModel
            {
                Id = u.Id,
                Username = u.UserName ?? string.Empty,
                Email = u.Email ?? string.Empty
            });
        }

        return View(new ReassignNoteViewModel
        {
            NoteId = noteId,
            NoteTitle = note.Title,
            CurrentOwner = note.User?.UserName ?? string.Empty,
            Users = users
        });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote(ReassignNoteViewModel model)
    {
        if (!ModelState.IsValid) return View(model);

        var note = await _db.Notes.FirstOrDefaultAsync(n => n.Id == model.NoteId);
        if (note == null) return NotFound();

        var newOwner = await _userManager.FindByIdAsync(model.NewOwnerId);
        if (newOwner == null)
        {
            ModelState.AddModelError(string.Empty, "User not found.");
            return View(model);
        }

        var oldOwner = note.UserId;
        note.UserId = model.NewOwnerId;
        await _db.SaveChangesAsync();

        _logger.LogInformation("Admin {Admin} reassigned note {NoteId} from {OldOwner} to {NewOwner}",
            User.Identity?.Name, model.NoteId, oldOwner, model.NewOwnerId);

        return RedirectToAction(nameof(Index));
    }
}
