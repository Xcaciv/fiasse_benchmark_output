using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

/// <summary>
/// File attachment metadata. Actual file stored under Uploads/ outside wwwroot.
/// Access gated through AttachmentsController (Authenticity + Integrity).
/// </summary>
public class Attachment
{
    /// <summary>Allowed file extensions — enforced at upload boundary (Integrity).</summary>
    public static readonly string[] AllowedExtensions =
        { ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg" };

    /// <summary>Max file size: 10 MB (Availability).</summary>
    public const long MaxFileSizeBytes = 10_485_760;

    public int Id { get; set; }

    public int NoteId { get; set; }
    public Note? Note { get; set; }

    [Required]
    [MaxLength(255)]
    public string OriginalFileName { get; set; } = string.Empty;

    /// <summary>GUID-based stored name — decouples storage path from user input (Integrity).</summary>
    [Required]
    [MaxLength(255)]
    public string StoredFileName { get; set; } = string.Empty;

    [Required]
    [MaxLength(100)]
    public string ContentType { get; set; } = string.Empty;

    public long FileSizeBytes { get; set; }
    public DateTime UploadedAt { get; set; }
}
