namespace LooseNotes.Models;

/// <summary>
/// Core note entity. Visibility defaults to private (server-enforced, FIASSE S2.1).
/// UserId is always set from the authenticated session, never from client input (Derived Integrity Principle).
/// </summary>
public sealed class Note
{
    public int Id { get; set; }
    public string UserId { get; set; } = string.Empty;
    public string Title { get; set; } = string.Empty;
    public string Content { get; set; } = string.Empty;

    /// <summary>
    /// Default is false (private). Server sets this default; client cannot override at creation.
    /// </summary>
    public bool IsPublic { get; set; } = false;

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // Computed aggregates - maintained server-side, never client-supplied
    public double AverageRating { get; set; } = 0;
    public int RatingCount { get; set; } = 0;

    // Navigation properties
    public ApplicationUser User { get; set; } = null!;
    public ICollection<Attachment> Attachments { get; set; } = [];
    public ICollection<Rating> Ratings { get; set; } = [];
    public ICollection<ShareLink> ShareLinks { get; set; } = [];
}
