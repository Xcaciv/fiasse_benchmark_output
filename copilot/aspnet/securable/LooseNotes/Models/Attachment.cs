using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class Attachment
{
    public int Id { get; set; }

    public int NoteId { get; set; }
    public Note Note { get; set; } = null!;

    /// <summary>Unique server-generated name (Guid + extension) — prevents path traversal.</summary>
    [Required]
    [MaxLength(500)]
    public string StoredFileName { get; set; } = string.Empty;

    /// <summary>Original file name preserved for display purposes only, never used for I/O.</summary>
    [Required]
    [MaxLength(255)]
    public string OriginalFileName { get; set; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string ContentType { get; set; } = string.Empty;

    public long FileSizeBytes { get; set; }

    public DateTimeOffset UploadedAt { get; set; } = DateTimeOffset.UtcNow;

    [Required]
    public string UploadedById { get; set; } = string.Empty;
}
