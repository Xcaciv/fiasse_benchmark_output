using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;

namespace LooseNotes.Models;

/// <summary>
/// File attachment metadata.
/// SSEM: Stored filename (on disk) is a GUID-based name; original name is metadata only.
/// This prevents directory traversal and name-collision attacks.
/// </summary>
public class Attachment
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    [ForeignKey(nameof(NoteId))]
    public Note Note { get; set; } = null!;

    /// <summary>Original filename supplied by the uploader – displayed to users but never used for disk I/O.</summary>
    [Required, MaxLength(260)]
    public string OriginalFileName { get; set; } = string.Empty;

    /// <summary>GUID-based filename stored on disk – never derived from user input.</summary>
    [Required, MaxLength(60)]
    public string StoredFileName { get; set; } = string.Empty;

    [Required, MaxLength(20)]
    public string ContentType { get; set; } = string.Empty;

    public long FileSizeBytes { get; set; }

    public DateTime UploadedAt { get; set; } = DateTime.UtcNow;

    public string UploadedById { get; set; } = string.Empty;

    [ForeignKey(nameof(UploadedById))]
    public ApplicationUser UploadedBy { get; set; } = null!;
}
