using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using LooseNotes.Services;

namespace LooseNotes.Controllers;

[Authorize(Roles = "Admin")]
public class AdminController : Controller
{
    private readonly IAdminService _adminService;
    private readonly ILogger<AdminController> _logger;

    public AdminController(IAdminService adminService, ILogger<AdminController> logger)
    {
        _adminService = adminService;
        _logger = logger;
    }

    [HttpGet]
    public async Task<IActionResult> Index()
    {
        var model = new AdminDashboardViewModel
        {
            TotalUsers = await _adminService.GetTotalUserCountAsync(),
            TotalNotes = await _adminService.GetTotalNoteCountAsync(),
            RecentActivity = await _adminService.GetRecentActivityAsync(20)
        };

        return View(model);
    }

    [HttpGet]
    public async Task<IActionResult> Users(string? search)
    {
        var users = await _adminService.GetAllUsersAsync(search);
        ViewData["Search"] = search;
        return View(users);
    }

    [HttpGet]
    public async Task<IActionResult> UserDetails(string id)
    {
        var user = await _adminService.GetUserByIdAsync(id);
        if (user == null)
        {
            return NotFound();
        }

        return View(user);
    }

    [HttpGet]
    public async Task<IActionResult> ReassignNote(int noteId)
    {
        var users = await _adminService.GetAllUsersAsync();
        
        var model = new ReassignNoteViewModel
        {
            NoteId = noteId,
            AvailableUsers = users
        };

        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote(ReassignNoteViewModel model)
    {
        if (!ModelState.IsValid)
        {
            model.AvailableUsers = await _adminService.GetAllUsersAsync();
            return View(model);
        }

        var result = await _adminService.ReassignNoteOwnershipAsync(
            model.NoteId, 
            model.NewOwnerId);

        if (!result)
        {
            ModelState.AddModelError(string.Empty, "Failed to reassign note ownership.");
            model.AvailableUsers = await _adminService.GetAllUsersAsync();
            return View(model);
        }

        await _adminService.LogActivityAsync(
            "NOTE_REASSIGNED",
            User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value,
            model.NoteId.ToString(),
            "Note");

        _logger.LogWarning("Note {NoteId} reassigned to user {NewOwnerId} by admin", 
            model.NoteId, model.NewOwnerId);

        TempData["SuccessMessage"] = "Note ownership reassigned successfully.";
        
        return RedirectToAction("Index");
    }

    [HttpGet]
    public async Task<IActionResult> Activity()
    {
        var activity = await _adminService.GetRecentActivityAsync(100);
        return View(activity);
    }
}

public class AdminDashboardViewModel
{
    public int TotalUsers { get; set; }
    public int TotalNotes { get; set; }
    public IEnumerable<Models.ActivityLog> RecentActivity { get; set; } = 
        Enumerable.Empty<Models.ActivityLog>();
}

public class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    
    [Required]
    public string NewOwnerId { get; set; } = string.Empty;

    public IEnumerable<Models.ApplicationUser> AvailableUsers { get; set; } = 
        Enumerable.Empty<Models.ApplicationUser>();
}
