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

    public AdminController(ApplicationDbContext db, UserManager<ApplicationUser> userManager,
        ILogger<AdminController> logger)
    {
        _db = db;
        _userManager = userManager;
        _logger = logger;
    }

    public async Task<IActionResult> Index(string? search)
    {
        var usersQuery = _userManager.Users.AsQueryable();
        if (!string.IsNullOrWhiteSpace(search))
        {
            var lower = search.ToLower();
            usersQuery = usersQuery.Where(u =>
                u.UserName!.ToLower().Contains(lower) ||
                u.Email!.ToLower().Contains(lower));
        }

        var users = await usersQuery.ToListAsync();
        var noteCounts = await _db.Notes
            .GroupBy(n => n.UserId)
            .Select(g => new { g.Key, Count = g.Count() })
            .ToDictionaryAsync(x => x.Key, x => x.Count);

        var vm = new AdminDashboardViewModel
        {
            TotalUsers = await _userManager.Users.CountAsync(),
            TotalNotes = await _db.Notes.CountAsync(),
            SearchQuery = search,
            Users = users.Select(u => new AdminUserRow
            {
                Id = u.Id,
                UserName = u.UserName ?? string.Empty,
                Email = u.Email ?? string.Empty,
                CreatedAt = u.CreatedAt,
                NoteCount = noteCounts.GetValueOrDefault(u.Id, 0)
            }).ToList()
        };
        return View(vm);
    }

    public async Task<IActionResult> Users(string? search)
    {
        return await Index(search);
    }

    [HttpGet]
    public async Task<IActionResult> ReassignNote(int noteId)
    {
        var note = await _db.Notes.Include(n => n.User).FirstOrDefaultAsync(n => n.Id == noteId);
        if (note == null) return NotFound();

        var allUsers = await _userManager.Users.ToListAsync();
        return View(new ReassignNoteViewModel
        {
            NoteId = note.Id,
            NoteTitle = note.Title,
            CurrentOwnerName = note.User?.UserName ?? "Unknown",
            AllUsers = allUsers
        });
    }

    [HttpPost, ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote(ReassignNoteViewModel model)
    {
        var note = await _db.Notes.FindAsync(model.NoteId);
        if (note == null) return NotFound();

        var newOwner = await _userManager.FindByIdAsync(model.NewOwnerId);
        if (newOwner == null)
        {
            ModelState.AddModelError(string.Empty, "User not found.");
            model.AllUsers = await _userManager.Users.ToListAsync();
            return View(model);
        }

        var oldOwner = note.UserId;
        note.UserId = model.NewOwnerId;
        await _db.SaveChangesAsync();

        _logger.LogInformation("Admin {Admin} reassigned note {NoteId} from {Old} to {New}.",
            User.Identity?.Name, note.Id, oldOwner, model.NewOwnerId);

        TempData["Success"] = "Note reassigned successfully.";
        return RedirectToAction(nameof(Index));
    }
}
