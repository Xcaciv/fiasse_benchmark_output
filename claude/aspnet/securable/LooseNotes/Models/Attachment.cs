using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// File attachment linked to a note.
/// StoredFileName is server-generated (UUID); OriginalFileName is display-only metadata.
/// Never serve StoredFileName directly to the client (Confidentiality, Integrity).
/// </summary>
public class Attachment
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    /// <summary>
    /// User-supplied original filename stored as metadata only.
    /// Sanitized on upload; never used for file system operations.
    /// </summary>
    [Required, MaxLength(255)]
    public string OriginalFileName { get; set; } = string.Empty;

    /// <summary>
    /// Server-generated UUID filename used for disk storage.
    /// Keeps storage path opaque to clients (Confidentiality).
    /// </summary>
    [Required, MaxLength(100)]
    public string StoredFileName { get; set; } = string.Empty;

    [Required, MaxLength(100)]
    public string ContentType { get; set; } = string.Empty;

    public long FileSizeBytes { get; set; }

    public DateTime UploadedAt { get; set; } = DateTime.UtcNow;

    public string UploadedById { get; set; } = string.Empty;

    // Navigation
    public Note? Note { get; set; }
    public ApplicationUser? UploadedBy { get; set; }
}
