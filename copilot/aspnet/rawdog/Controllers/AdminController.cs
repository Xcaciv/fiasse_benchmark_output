using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using rawdog.Data;
using rawdog.Services;
using rawdog.ViewModels;

namespace rawdog.Controllers;

[Authorize(Roles = "Admin")]
public sealed class AdminController(ApplicationDbContext dbContext, IActivityLogger activityLogger) : Controller
{
    public async Task<IActionResult> Dashboard(string? q, CancellationToken cancellationToken)
    {
        var searchTerm = (q ?? string.Empty).Trim();
        var pattern = $"%{searchTerm}%";

        var usersQuery = dbContext.Users.AsQueryable();
        if (searchTerm.Length > 0)
        {
            usersQuery = usersQuery.Where(user =>
                EF.Functions.Like(user.UserName!, pattern) ||
                EF.Functions.Like(user.Email!, pattern));
        }

        var userOptions = await dbContext.Users
            .OrderBy(user => user.UserName)
            .Select(user => new AdminUserOptionViewModel
            {
                Id = user.Id,
                UserName = user.UserName ?? "Unknown"
            })
            .ToListAsync(cancellationToken);

        var model = new AdminDashboardViewModel
        {
            SearchTerm = searchTerm,
            TotalUsers = await dbContext.Users.CountAsync(cancellationToken),
            TotalNotes = await dbContext.Notes.CountAsync(cancellationToken),
            Users = await usersQuery
                .OrderBy(user => user.UserName)
                .Select(user => new AdminUserItemViewModel
                {
                    Id = user.Id,
                    UserName = user.UserName ?? "Unknown",
                    Email = user.Email ?? string.Empty,
                    RegisteredAtUtc = user.RegisteredAtUtc,
                    NoteCount = user.Notes.Count
                })
                .Take(50)
                .ToListAsync(cancellationToken),
            RecentActivity = await dbContext.ActivityLogs
                .Include(log => log.User)
                .OrderByDescending(log => log.CreatedAtUtc)
                .Take(20)
                .Select(log => new AdminActivityItemViewModel
                {
                    ActionType = log.ActionType,
                    Message = log.Message,
                    UserName = log.User != null ? (log.User.UserName ?? "Unknown") : "System",
                    CreatedAtUtc = log.CreatedAtUtc
                })
                .ToListAsync(cancellationToken),
            RecentNotes = await dbContext.Notes
                .Include(note => note.Owner)
                .OrderByDescending(note => note.UpdatedAtUtc ?? note.CreatedAtUtc)
                .Take(20)
                .Select(note => new AdminNoteItemViewModel
                {
                    Id = note.Id,
                    Title = note.Title,
                    OwnerId = note.OwnerId,
                    OwnerUserName = note.Owner!.UserName ?? "Unknown",
                    IsPublic = note.IsPublic,
                    UpdatedOrCreatedAtUtc = note.UpdatedAtUtc ?? note.CreatedAtUtc
                })
                .ToListAsync(cancellationToken),
            UserOptions = userOptions
        };

        return View(model);
    }

    [HttpPost]
    [ValidateAntiForgeryToken]
    public async Task<IActionResult> ReassignNote(int noteId, string newOwnerId, CancellationToken cancellationToken)
    {
        var note = await dbContext.Notes.Include(item => item.Owner).SingleOrDefaultAsync(item => item.Id == noteId, cancellationToken);
        if (note is null)
        {
            return NotFound();
        }

        var newOwner = await dbContext.Users.SingleOrDefaultAsync(user => user.Id == newOwnerId, cancellationToken);
        if (newOwner is null)
        {
            TempData["ErrorMessage"] = "The selected user could not be found.";
            return RedirectToAction(nameof(Dashboard));
        }

        var previousOwner = note.Owner?.UserName ?? "Unknown";
        note.OwnerId = newOwner.Id;
        note.UpdatedAtUtc = DateTime.UtcNow;
        await dbContext.SaveChangesAsync(cancellationToken);

        await activityLogger.LogAsync(
            "admin.note_reassigned",
            $"Reassigned note '{note.Title}' from '{previousOwner}' to '{newOwner.UserName}'.",
            newOwner.Id,
            cancellationToken);

        TempData["StatusMessage"] = "Note ownership updated.";
        return RedirectToAction(nameof(Dashboard));
    }
}
