using LooseNotes.Models;
using LooseNotes.ViewModels.Notes;

namespace LooseNotes.Services.Interfaces;

/// <summary>
/// Core note operations. All mutations enforce ownership before proceeding (Integrity).
/// </summary>
public interface INoteService
{
    /// <summary>Returns note if requester has access (owner or public), null otherwise.</summary>
    Task<Note?> GetNoteAsync(int id, string? requestingUserId);

    Task<IEnumerable<Note>> GetUserNotesAsync(string userId);

    /// <summary>Searches title+content; owned notes any visibility, public from others only.</summary>
    Task<IEnumerable<Note>> SearchNotesAsync(string query, string? requestingUserId);

    Task<Note> CreateNoteAsync(NoteCreateViewModel model, string userId);

    /// <summary>Returns false if note not found or requester not owner.</summary>
    Task<bool> UpdateNoteAsync(int id, NoteEditViewModel model, string userId);

    /// <summary>Cascade-deletes attachments, ratings, share links. Admin bypass ownership check.</summary>
    Task<bool> DeleteNoteAsync(int id, string userId, bool isAdmin);

    Task<IEnumerable<Note>> GetTopRatedNotesAsync(int minRatings = 3);
}
