using System.ComponentModel.DataAnnotations;

namespace LooseNotes.Models;

public class ActivityLog
{
    [Key]
    public int Id { get; set; }

    [Required]
    [MaxLength(100)]
    public string Action { get; set; } = string.Empty;

    [MaxLength(500)]
    public string? Details { get; set; }

    public string? UserId { get; set; }

    public string? TargetId { get; set; }

    [MaxLength(50)]
    public string? TargetType { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
