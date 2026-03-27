// Attachment.cs — File attachment linked to a Note.
// Confidentiality: StoredFileName is a generated UUID — original name is metadata only.
// Integrity: extension and size validated before entity creation (see LocalFileStorageService).
namespace LooseNotes.Models;

/// <summary>Metadata record for a file attached to a note.
/// The actual file lives at [StorageBasePath]/[StoredFileName].</summary>
public sealed class Attachment
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    /// <summary>Original user-provided filename — displayed only, never used for FS operations.</summary>
    public required string OriginalFileName { get; set; }

    /// <summary>UUID-based name used for actual file system storage. Prevents path traversal.</summary>
    public required string StoredFileName { get; set; }

    /// <summary>MIME content type recorded at upload time.</summary>
    public required string ContentType { get; set; }

    /// <summary>File size in bytes, recorded at upload time.</summary>
    public long FileSizeBytes { get; set; }

    public DateTime UploadedAt { get; init; } = DateTime.UtcNow;

    // ── Navigation ────────────────────────────────────────────────────────────
    public Note? Note { get; set; }
}
