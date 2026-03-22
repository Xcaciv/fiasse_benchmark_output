namespace LooseNotes.Configuration;

/// <summary>
/// Strongly-typed configuration for file upload constraints (Integrity, Availability).
/// Bound from "FileStorage" section in appsettings.json.
/// </summary>
public class FileStorageOptions
{
    public const string SectionName = "FileStorage";

    public string UploadPath { get; set; } = "uploads";

    /// <summary>Maximum accepted file size in bytes. Default: 10 MB.</summary>
    public long MaxFileSizeBytes { get; set; } = 10_485_760;

    /// <summary>
    /// Allow-list of permitted file extensions (lowercase with leading dot).
    /// Only extensions in this list are accepted at the trust boundary.
    /// </summary>
    public HashSet<string> AllowedExtensions { get; set; } =
        new(StringComparer.OrdinalIgnoreCase)
        {
            ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg"
        };
}
