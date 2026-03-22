using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.Rendering;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Services;
using LooseNotes.ViewModels.Admin;

namespace LooseNotes.Controllers;

[Authorize(Roles = "Admin")]
public sealed class AdminController : Controller
{
    private readonly ApplicationDbContext _dbContext;
    private readonly IActivityLogService _activityLogService;

    public AdminController(ApplicationDbContext dbContext, IActivityLogService activityLogService)
    {
        _dbContext = dbContext;
        _activityLogService = activityLogService;
    }

    [HttpGet]
    public async Task<IActionResult> Dashboard(CancellationToken cancellationToken)
    {
        var recentActivity = await (
            from activity in _dbContext.ActivityLogs.AsNoTracking()
            join user in _dbContext.Users.AsNoTracking() on activity.ActorUserId equals user.Id into users
            from actor in users.DefaultIfEmpty()
            orderby activity.CreatedAtUtc descending
            select new ActivityLogItemViewModel
            {
                CreatedAtUtc = activity.CreatedAtUtc,
                ActionType = activity.ActionType,
                Description = activity.Description,
                ActorUserName = actor != null ? actor.UserName : null
            })
            .Take(25)
            .ToListAsync(cancellationToken);

        var model = new DashboardViewModel
        {
            TotalUserCount = await _dbContext.Users.CountAsync(cancellationToken),
            TotalNoteCount = await _dbContext.Notes.CountAsync(cancellationToken),
            RecentActivity = recentActivity
        };

        return View(model);
    }

    [HttpGet]
    public async Task<IActionResult> Users(string? query, CancellationToken cancellationToken)
    {
        var normalizedQuery = query?.Trim();
        var usersQuery = _dbContext.Users.AsNoTracking();
        if (!string.IsNullOrWhiteSpace(normalizedQuery))
        {
            var pattern = $"%{normalizedQuery}%";
            usersQuery = usersQuery.Where(x => EF.Functions.Like(x.UserName!, pattern) || EF.Functions.Like(x.Email!, pattern));
        }

        var users = await usersQuery
            .OrderBy(x => x.UserName)
            .Select(x => new UserListItemViewModel
            {
                Id = x.Id,
                UserName = x.UserName ?? string.Empty,
                Email = x.Email ?? string.Empty,
                RegisteredAtUtc = x.RegisteredAtUtc,
                NoteCount = x.OwnedNotes.Count
            })
            .ToListAsync(cancellationToken);

        return View(new UsersViewModel { Query = normalizedQuery, Users = users });
    }

    [HttpGet]
    public async Task<IActionResult> Reassign(int id, CancellationToken cancellationToken)
    {
        var note = await _dbContext.Notes
            .AsNoTracking()
            .Include(x => x.Owner)
            .SingleOrDefaultAsync(x => x.Id == id, cancellationToken);
        if (note is null)
        {
            return NotFound();
        }

        return View(await BuildReassignViewModelAsync(note.Id, note.Title, note.Owner.UserName ?? "Unknown", cancellationToken));
    }

    [HttpPost]
    public async Task<IActionResult> Reassign(ReassignOwnerViewModel model, CancellationToken cancellationToken)
    {
        var note = await _dbContext.Notes
            .Include(x => x.Owner)
            .SingleOrDefaultAsync(x => x.Id == model.NoteId, cancellationToken);
        if (note is null)
        {
            return NotFound();
        }

        if (!ModelState.IsValid)
        {
            return View(await BuildReassignViewModelAsync(note.Id, note.Title, note.Owner.UserName ?? "Unknown", cancellationToken, model.NewOwnerId));
        }

        var newOwner = await _dbContext.Users.SingleOrDefaultAsync(x => x.Id == model.NewOwnerId, cancellationToken);
        if (newOwner is null)
        {
            ModelState.AddModelError(nameof(model.NewOwnerId), "Select a valid user.");
            return View(await BuildReassignViewModelAsync(note.Id, note.Title, note.Owner.UserName ?? "Unknown", cancellationToken, model.NewOwnerId));
        }

        note.OwnerId = newOwner.Id;
        note.UpdatedAtUtc = DateTime.UtcNow;
        await _dbContext.SaveChangesAsync(cancellationToken);

        await _activityLogService.LogAsync("admin.note_reassigned", $"Note '{note.Title}' was reassigned to '{newOwner.UserName}'.", User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value, HttpContext.Connection.RemoteIpAddress?.ToString(), cancellationToken);
        TempData["StatusMessage"] = "Note ownership was reassigned.";
        return RedirectToAction(nameof(Dashboard));
    }

    private async Task<ReassignOwnerViewModel> BuildReassignViewModelAsync(int noteId, string title, string currentOwnerUserName, CancellationToken cancellationToken, string? selectedUserId = null)
    {
        var users = await _dbContext.Users
            .AsNoTracking()
            .OrderBy(x => x.UserName)
            .Select(x => new SelectListItem
            {
                Value = x.Id,
                Text = $"{x.UserName} ({x.Email})",
                Selected = x.Id == selectedUserId
            })
            .ToListAsync(cancellationToken);

        return new ReassignOwnerViewModel
        {
            NoteId = noteId,
            NoteTitle = title,
            CurrentOwnerUserName = currentOwnerUserName,
            NewOwnerId = selectedUserId ?? string.Empty,
            Users = users
        };
    }
}
