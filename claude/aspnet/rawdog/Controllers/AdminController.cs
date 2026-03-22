using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.ViewModels;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.EntityFrameworkCore;

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

    // GET: Admin/Index - Dashboard
    public async Task<IActionResult> Index()
    {
        var totalUsers = await _db.Users.CountAsync();
        var totalNotes = await _db.Notes.CountAsync();
        var totalPublicNotes = await _db.Notes.CountAsync(n => n.IsPublic);
        var totalRatings = await _db.Ratings.CountAsync();

        var vm = new AdminDashboardViewModel
        {
            TotalUsers = totalUsers,
            TotalNotes = totalNotes,
            TotalPublicNotes = totalPublicNotes,
            TotalRatings = totalRatings
        };

        return View(vm);
    }

    // GET: Admin/Users
    public async Task<IActionResult> Users(string? q)
    {
        var usersQuery = _db.Users.AsQueryable();

        if (!string.IsNullOrWhiteSpace(q))
        {
            var lower = q.ToLower();
            usersQuery = usersQuery.Where(u =>
                u.UserName!.ToLower().Contains(lower) ||
                u.Email!.ToLower().Contains(lower));
        }

        var users = await usersQuery
            .Include(u => u.Notes)
            .OrderBy(u => u.UserName)
            .ToListAsync();

        var items = new List<AdminUserItem>();
        foreach (var u in users)
        {
            var roles = await _userManager.GetRolesAsync(u);
            items.Add(new AdminUserItem
            {
                Id = u.Id,
                UserName = u.UserName!,
                Email = u.Email!,
                CreatedAt = u.CreatedAt,
                NoteCount = u.Notes.Count,
                Roles = roles.ToList()
            });
        }

        return View(new AdminUserListViewModel { SearchQuery = q, Users = items });
    }

    // GET: Admin/ReassignNote/5
    public async Task<IActionResult> ReassignNote(int id)
    {
        var note = await _db.Notes.Include(n => n.Owner).FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var users = await _db.Users.OrderBy(u => u.UserName).ToListAsync();

        return View(new ReassignNoteViewModel
        {
            NoteId = note.Id,
            NoteTitle = note.Title,
            CurrentOwnerId = note.OwnerId,
            CurrentOwnerName = note.Owner?.UserName ?? "",
            Users = new SelectList(users, "Id", "UserName", note.OwnerId)
        });
    }

    // POST: Admin/ReassignNote
    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote(ReassignNoteViewModel model)
    {
        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note == null) return NotFound();

        var newOwner = await _userManager.FindByIdAsync(model.NewOwnerId);
        if (newOwner == null)
        {
            ModelState.AddModelError("NewOwnerId", "Selected user not found.");
            var users = await _db.Users.OrderBy(u => u.UserName).ToListAsync();
            model.Users = new SelectList(users, "Id", "UserName");
            return View(model);
        }

        var adminId = _userManager.GetUserId(User);
        var oldOwnerId = note.OwnerId;

        note.OwnerId = model.NewOwnerId;
        note.UpdatedAt = DateTime.UtcNow;
        await _db.SaveChangesAsync();

        _logger.LogInformation(
            "Admin {AdminId} reassigned note {NoteId} from user {OldOwner} to {NewOwner}",
            adminId, note.Id, oldOwnerId, model.NewOwnerId);

        TempData["Success"] = $"Note '{note.Title}' reassigned to {newOwner.UserName}.";
        return RedirectToAction(nameof(Users));
    }
}
