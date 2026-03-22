namespace LooseNotes.Models;

public sealed class ShareLink
{
    public int Id { get; set; }

    public int NoteId { get; set; }
    public Note Note { get; set; } = null!;

    public string TokenHash { get; set; } = string.Empty;
    public string ProtectedToken { get; set; } = string.Empty;

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
    public DateTime? RevokedAtUtc { get; set; }

    public bool IsRevoked => RevokedAtUtc.HasValue;
}
