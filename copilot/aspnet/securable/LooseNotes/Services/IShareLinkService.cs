using LooseNotes.Models;

namespace LooseNotes.Services;

public interface IShareLinkService
{
    Task<ShareLink> CreateShareLinkAsync(int noteId, string createdById, CancellationToken ct = default);
    Task<bool> RevokeShareLinkAsync(int shareLinkId, string requestingUserId, CancellationToken ct = default);
    Task<Note?> GetNoteByTokenAsync(string token, CancellationToken ct = default);
}
