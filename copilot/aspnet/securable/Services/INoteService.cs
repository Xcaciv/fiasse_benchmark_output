using LooseNotes.Models;
using LooseNotes.ViewModels.Notes;
using Microsoft.AspNetCore.Http;

namespace LooseNotes.Services;

public interface INoteService
{
    Task<List<NoteListItemViewModel>> GetUserNotesAsync(string userId, CancellationToken cancellationToken = default);
    Task<NoteDetailViewModel?> GetNoteDetailAsync(int noteId, string? currentUserId, CancellationToken cancellationToken = default);
    Task<Note?> GetNoteForEditAsync(int noteId, string ownerId, CancellationToken cancellationToken = default);
    Task<Note> CreateNoteAsync(string ownerId, NoteViewModel model, CancellationToken cancellationToken = default);
    Task<bool> UpdateNoteAsync(int noteId, string ownerId, NoteViewModel model, CancellationToken cancellationToken = default);
    Task<bool> DeleteNoteAsync(int noteId, string ownerId, CancellationToken cancellationToken = default);
    Task<List<NoteListItemViewModel>> SearchNotesAsync(string query, string? currentUserId, CancellationToken cancellationToken = default);
    Task<List<NoteListItemViewModel>> GetTopRatedNotesAsync(int minRatings = 3, CancellationToken cancellationToken = default);
    Task<bool> AddAttachmentAsync(int noteId, string ownerId, IFormFile file, CancellationToken cancellationToken = default);
    Task<bool> DeleteAttachmentAsync(int attachmentId, string ownerId, CancellationToken cancellationToken = default);
    Task<Attachment?> GetAttachmentAsync(int attachmentId, string? currentUserId, CancellationToken cancellationToken = default);
    Task<bool> AddOrUpdateRatingAsync(string raterId, RatingViewModel model, CancellationToken cancellationToken = default);
}
