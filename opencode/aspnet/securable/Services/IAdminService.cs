using LooseNotes.Models;

namespace LooseNotes.Services;

public interface IAdminService
{
    Task<int> GetTotalUserCountAsync();
    Task<int> GetTotalNoteCountAsync();
    Task<IEnumerable<ApplicationUser>> GetAllUsersAsync(string? searchTerm = null);
    Task<ApplicationUser?> GetUserByIdAsync(string userId);
    Task<bool> ReassignNoteOwnershipAsync(int noteId, string newOwnerId);
    Task LogActivityAsync(string action, string? userId = null, string? targetId = null, string? targetType = null);
    Task<IEnumerable<ActivityLog>> GetRecentActivityAsync(int count = 50);
}
