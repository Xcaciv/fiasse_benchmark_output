using System.ComponentModel.DataAnnotations;

namespace rawdog.Models;

public sealed class NoteAttachment
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    public Note? Note { get; set; }

    [Required]
    [StringLength(260)]
    public string OriginalFileName { get; set; } = string.Empty;

    [Required]
    [StringLength(260)]
    public string StoredFileName { get; set; } = string.Empty;

    [Required]
    [StringLength(150)]
    public string ContentType { get; set; } = "application/octet-stream";

    public long SizeBytes { get; set; }

    public DateTime UploadedAtUtc { get; set; } = DateTime.UtcNow;
}
