using LooseNotes.Models;

namespace LooseNotes.Services;

public interface IShareLinkService
{
    Task<ShareLink> GenerateShareLinkAsync(int noteId, string ownerId, CancellationToken cancellationToken = default);
    Task<bool> RevokeShareLinkAsync(int noteId, string ownerId, CancellationToken cancellationToken = default);
    Task<Note?> GetNoteByTokenAsync(string token, CancellationToken cancellationToken = default);
}
