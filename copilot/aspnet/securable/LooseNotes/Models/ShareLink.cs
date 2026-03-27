using System.ComponentModel.DataAnnotations;
using System.Security.Cryptography;

namespace LooseNotes.Models;

public class ShareLink
{
    public int Id { get; set; }

    public int NoteId { get; set; }
    public Note Note { get; set; } = null!;

    /// <summary>
    /// Cryptographically random URL-safe token. Set via <see cref="GenerateToken"/>.
    /// Trust boundary: token validated on every share-link lookup before note access.
    /// </summary>
    [Required]
    [MaxLength(100)]
    public string Token { get; set; } = string.Empty;

    public DateTimeOffset CreatedAt { get; set; } = DateTimeOffset.UtcNow;

    [Required]
    public string CreatedById { get; set; } = string.Empty;

    /// <summary>
    /// Generates a cryptographically random, URL-safe base64 token.
    /// </summary>
    public static string GenerateToken()
    {
        var bytes = RandomNumberGenerator.GetBytes(32);
        return Convert.ToBase64String(bytes)
            .Replace("+", "-")
            .Replace("/", "_")
            .TrimEnd('=');
    }
}
