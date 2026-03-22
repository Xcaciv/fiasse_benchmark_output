using LooseNotes.Models;

namespace LooseNotes.Services.Interfaces;

/// <summary>
/// Share link lifecycle management. Token uniqueness guaranteed by DB unique index (Authenticity).
/// </summary>
public interface IShareLinkService
{
    /// <summary>Creates a new non-expiring share link for the note owned by userId.</summary>
    Task<ShareLink> CreateShareLinkAsync(int noteId, string userId);

    /// <summary>Revokes all active share links for the note. Returns false if none found or not owner.</summary>
    Task<bool> RevokeShareLinkAsync(int noteId, string userId);

    /// <summary>
    /// Resolves a share token to the associated Note.
    /// Returns null if token not found, revoked, or expired.
    /// </summary>
    Task<Note?> GetNoteByShareTokenAsync(string token);
}
