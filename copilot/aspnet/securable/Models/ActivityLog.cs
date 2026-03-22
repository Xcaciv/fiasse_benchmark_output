using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public sealed class ActivityLog
{
    public int Id { get; set; }

    [StringLength(64)]
    public string ActionType { get; set; } = string.Empty;

    [StringLength(512)]
    public string Description { get; set; } = string.Empty;

    public string? ActorUserId { get; set; }

    [StringLength(64)]
    public string? IpAddress { get; set; }

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
}
