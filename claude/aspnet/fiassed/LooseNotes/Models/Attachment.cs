namespace LooseNotes.Models;

/// <summary>
/// File attachment metadata. StoredFileName is server-generated (UUID);
/// OriginalFileName is display-only metadata, never used in file system paths (ASVS V5.3.2).
/// </summary>
public sealed class Attachment
{
    public int Id { get; set; }
    public int NoteId { get; set; }
    public string UserId { get; set; } = string.Empty;

    /// <summary>
    /// UUID-based name used for file system storage. Never derived from user input.
    /// </summary>
    public string StoredFileName { get; set; } = string.Empty;

    /// <summary>
    /// Original filename stored as metadata only. Never used to construct file paths.
    /// </summary>
    public string OriginalFileName { get; set; } = string.Empty;

    public string ContentType { get; set; } = string.Empty;
    public long FileSizeBytes { get; set; }
    public DateTime UploadedAt { get; set; } = DateTime.UtcNow;

    // Navigation properties
    public Note Note { get; set; } = null!;
    public ApplicationUser User { get; set; } = null!;
}
