using LooseNotes.Models;

namespace LooseNotes.Services;

public interface INoteService
{
    Task<Note> CreateNoteAsync(Note note);
    Task<Note?> GetNoteByIdAsync(int id);
    Task<Note?> GetNoteByIdForUserAsync(int id, string userId);
    Task<Note?> GetPublicNoteByIdAsync(int id);
    Task<Note?> GetSharedNoteByTokenAsync(string token);
    Task<IEnumerable<Note>> GetUserNotesAsync(string userId);
    Task<IEnumerable<Note>> GetPublicNotesAsync();
    Task<IEnumerable<Note>> GetTopRatedNotesAsync(int minimumRatings = 3);
    Task<Note> UpdateNoteAsync(Note note);
    Task<bool> DeleteNoteAsync(int id, string userId, bool isAdmin);
    Task<ShareLink> CreateShareLinkAsync(int noteId);
    Task<ShareLink?> GetActiveShareLinkAsync(int noteId);
    Task<bool> RevokeShareLinkAsync(int noteId, string userId);
    Task<IEnumerable<Rating>> GetNoteRatingsAsync(int noteId);
    Task<Rating> AddOrUpdateRatingAsync(int noteId, string userId, int value, string? comment);
    Task<bool> CanUserAccessNoteAsync(int noteId, string userId, bool isAdmin);
}
