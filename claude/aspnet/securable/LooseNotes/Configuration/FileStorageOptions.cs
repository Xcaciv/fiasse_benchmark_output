// FileStorageOptions.cs — Strongly-typed configuration for file uploads.
// Modifiability: centralised; changing limits requires only appsettings update.
namespace LooseNotes.Configuration;

/// <summary>Validated configuration for local file storage.
/// Bound from the "FileStorage" configuration section.</summary>
public sealed class FileStorageOptions
{
    public const string SectionName = "FileStorage";

    /// <summary>Base directory path for stored uploads.</summary>
    public string BasePath { get; set; } = "uploads";

    /// <summary>Maximum allowed file size in bytes (default 10 MB).</summary>
    public long MaxFileSizeBytes { get; set; } = 10 * 1024 * 1024;

    /// <summary>Permitted file extensions (lower-case, with leading dot).</summary>
    public IReadOnlyList<string> AllowedExtensions { get; set; } =
        new[] { ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg" };
}
