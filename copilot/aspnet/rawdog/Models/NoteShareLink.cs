using System.ComponentModel.DataAnnotations;

namespace rawdog.Models;

public sealed class NoteShareLink
{
    public int Id { get; set; }

    public int NoteId { get; set; }

    public Note? Note { get; set; }

    [Required]
    [StringLength(100)]
    public string Token { get; set; } = string.Empty;

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;

    public DateTime? RevokedAtUtc { get; set; }

    public bool IsActive => RevokedAtUtc is null;
}
