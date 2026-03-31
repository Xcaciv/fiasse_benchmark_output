using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;

namespace LooseNotes.Services;

public class AdminService : IAdminService
{
    private readonly ApplicationDbContext _context;
    private readonly UserManager<ApplicationUser> _userManager;
    private readonly ILogger<AdminService> _logger;

    public AdminService(
        ApplicationDbContext context,
        UserManager<ApplicationUser> userManager,
        ILogger<AdminService> logger)
    {
        _context = context;
        _userManager = userManager;
        _logger = logger;
    }

    public async Task<int> GetTotalUserCountAsync()
    {
        return await _context.Users.CountAsync();
    }

    public async Task<int> GetTotalNoteCountAsync()
    {
        return await _context.Notes.CountAsync();
    }

    public async Task<IEnumerable<ApplicationUser>> GetAllUsersAsync(string? searchTerm = null)
    {
        var query = _context.Users.AsQueryable();

        if (!string.IsNullOrWhiteSpace(searchTerm))
        {
            var term = searchTerm.ToLower();
            query = query.Where(u => 
                u.UserName!.ToLower().Contains(term) || 
                u.Email!.ToLower().Contains(term));
        }

        return await query
            .OrderByDescending(u => u.CreatedAt)
            .ToListAsync();
    }

    public async Task<ApplicationUser?> GetUserByIdAsync(string userId)
    {
        return await _context.Users
            .Include(u => u.Notes)
            .FirstOrDefaultAsync(u => u.Id == userId);
    }

    public async Task<bool> ReassignNoteOwnershipAsync(int noteId, string newOwnerId)
    {
        var note = await _context.Notes.FindAsync(noteId);
        if (note == null)
        {
            return false;
        }

        var newOwner = await _userManager.FindByIdAsync(newOwnerId);
        if (newOwner == null)
        {
            return false;
        }

        var oldOwnerId = note.UserId;
        note.UserId = newOwnerId;
        note.ModifiedAt = DateTime.UtcNow;

        await _context.SaveChangesAsync();
        
        _logger.LogWarning("Note ownership reassigned: Note {NoteId} from {OldOwner} to {NewOwner}", 
            noteId, oldOwnerId, newOwnerId);
        
        return true;
    }

    public async Task LogActivityAsync(string action, string? userId = null, string? targetId = null, string? targetType = null)
    {
        var log = new ActivityLog
        {
            Action = action,
            UserId = userId,
            TargetId = targetId,
            TargetType = targetType,
            CreatedAt = DateTime.UtcNow
        };

        _context.ActivityLogs.Add(log);
        await _context.SaveChangesAsync();
    }

    public async Task<IEnumerable<ActivityLog>> GetRecentActivityAsync(int count = 50)
    {
        return await _context.ActivityLogs
            .OrderByDescending(l => l.CreatedAt)
            .Take(count)
            .ToListAsync();
    }
}
