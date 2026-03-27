namespace LooseNotes.Models;

public class ActivityLog
{
    public int Id { get; set; }
    public string Action { get; set; } = string.Empty;
    public string? Details { get; set; }
    public string? UserId { get; set; }
    public string? TargetType { get; set; }
    public int? TargetId { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
