using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.ViewModels.Admin;

namespace LooseNotes.Controllers;

[Authorize(Roles = "Admin")]
public class AdminController : Controller
{
    private readonly ApplicationDbContext _context;
    private readonly UserManager<ApplicationUser> _userManager;

    public AdminController(ApplicationDbContext context, UserManager<ApplicationUser> userManager)
    {
        _context = context;
        _userManager = userManager;
    }

    public async Task<IActionResult> Dashboard()
    {
        var vm = new DashboardViewModel
        {
            TotalUsers = await _context.Users.CountAsync(),
            TotalNotes = await _context.Notes.CountAsync(),
            PublicNotes = await _context.Notes.CountAsync(n => n.IsPublic),
            RecentActivity = await _context.ActivityLogs
                .Include(a => a.User)
                .OrderByDescending(a => a.Timestamp)
                .Take(20)
                .ToListAsync()
        };
        return View(vm);
    }

    public async Task<IActionResult> Users()
    {
        var users = await _context.Users.ToListAsync();
        var list = new List<UserListViewModel>();

        foreach (var u in users)
        {
            var roles = await _userManager.GetRolesAsync(u);
            list.Add(new UserListViewModel
            {
                UserId = u.Id,
                UserName = u.UserName ?? string.Empty,
                Email = u.Email ?? string.Empty,
                CreatedAt = u.CreatedAt,
                NoteCount = await _context.Notes.CountAsync(n => n.UserId == u.Id),
                Roles = roles
            });
        }

        return View(list);
    }

    public async Task<IActionResult> UserDetail(string id)
    {
        var user = await _context.Users
            .Include(u => u.Notes)
            .FirstOrDefaultAsync(u => u.Id == id);
        if (user == null) return NotFound();

        ViewBag.Roles = await _userManager.GetRolesAsync(user);
        return View(user);
    }

    public async Task<IActionResult> Notes()
    {
        var notes = await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Ratings)
            .OrderByDescending(n => n.UpdatedAt)
            .ToListAsync();
        return View(notes);
    }

    [HttpGet]
    public async Task<IActionResult> NoteDetails(int id)
    {
        var note = await _context.Notes
            .Include(n => n.User)
            .Include(n => n.Attachments)
            .Include(n => n.Ratings).ThenInclude(r => r.User)
            .FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var allUsers = await _context.Users.ToListAsync();
        ViewBag.AllUsers = allUsers;
        return View(note);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote(int id, string newUserId)
    {
        var note = await _context.Notes.FirstOrDefaultAsync(n => n.Id == id);
        if (note == null) return NotFound();

        var newUser = await _userManager.FindByIdAsync(newUserId);
        if (newUser == null) return BadRequest("Invalid user.");

        var adminId = _userManager.GetUserId(User);
        note.UserId = newUserId;
        note.UpdatedAt = DateTime.UtcNow;
        await _context.SaveChangesAsync();

        _context.ActivityLogs.Add(new ActivityLog
        {
            Action = "ReassignNote",
            EntityType = "Note",
            EntityId = note.Id.ToString(),
            Timestamp = DateTime.UtcNow,
            UserId = adminId
        });
        await _context.SaveChangesAsync();

        TempData["Success"] = $"Note reassigned to {newUser.UserName}.";
        return RedirectToAction(nameof(NoteDetails), new { id });
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> DeleteUser(string id)
    {
        var user = await _userManager.FindByIdAsync(id);
        if (user == null) return NotFound();

        var adminId = _userManager.GetUserId(User);
        if (adminId == id) return BadRequest("Cannot delete yourself.");

        var notes = await _context.Notes.Include(n => n.Attachments).Where(n => n.UserId == id).ToListAsync();
        _context.Notes.RemoveRange(notes);
        await _context.SaveChangesAsync();

        await _userManager.DeleteAsync(user);

        _context.ActivityLogs.Add(new ActivityLog
        {
            Action = "DeleteUser",
            EntityType = "User",
            EntityId = id,
            Timestamp = DateTime.UtcNow,
            UserId = adminId
        });
        await _context.SaveChangesAsync();

        TempData["Success"] = "User deleted.";
        return RedirectToAction(nameof(Users));
    }
}
