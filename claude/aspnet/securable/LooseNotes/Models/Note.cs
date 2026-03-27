// Note.cs — Core domain entity representing a user note.
// Integrity: required fields use non-nullable types; no defaults that could mask missing data.
namespace LooseNotes.Models;

/// <summary>Visibility state of a note.</summary>
public enum NoteVisibility
{
    Private = 0,
    Public = 1
}

/// <summary>A user-authored note, optionally public and shareable.</summary>
public sealed class Note
{
    public int Id { get; set; }

    // Integrity: required string — EF will enforce NOT NULL in schema
    public required string Title { get; set; }

    public required string Content { get; set; }

    /// <summary>Owner FK — Integrity: always set, never nullable.</summary>
    public required string UserId { get; set; }

    public NoteVisibility Visibility { get; set; } = NoteVisibility.Private;

    public DateTime CreatedAt { get; init; } = DateTime.UtcNow;

    public DateTime UpdatedAt { get; set; } = DateTime.UtcNow;

    // ── Navigation properties ─────────────────────────────────────────────────
    public ApplicationUser? User { get; set; }

    public ICollection<Attachment> Attachments { get; init; } = new List<Attachment>();

    public ICollection<Rating> Ratings { get; init; } = new List<Rating>();

    public ICollection<ShareLink> ShareLinks { get; init; } = new List<ShareLink>();
}
