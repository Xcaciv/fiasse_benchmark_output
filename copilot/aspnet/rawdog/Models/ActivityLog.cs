using System.ComponentModel.DataAnnotations;

namespace rawdog.Models;

public sealed class ActivityLog
{
    public int Id { get; set; }

    [Required]
    [StringLength(100)]
    public string ActionType { get; set; } = string.Empty;

    [Required]
    [StringLength(500)]
    public string Message { get; set; } = string.Empty;

    public string? UserId { get; set; }

    public ApplicationUser? User { get; set; }

    public DateTime CreatedAtUtc { get; set; } = DateTime.UtcNow;
}
