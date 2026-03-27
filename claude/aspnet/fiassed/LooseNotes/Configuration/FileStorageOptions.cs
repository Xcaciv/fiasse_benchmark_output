namespace LooseNotes.Configuration;

/// <summary>
/// Externalized file storage configuration. Adding or removing allowed types
/// requires only configuration changes, not code changes (FIASSE S2.1).
/// </summary>
public sealed class FileStorageOptions
{
    public const string SectionName = "FileStorage";

    public string StoragePath { get; init; } = "uploads";
    public long MaxFileSizeBytes { get; init; } = 10_485_760; // 10 MB
    public int MaxAttachmentsPerUser { get; init; } = 100;
    public long MaxTotalBytesPerUser { get; init; } = 524_288_000; // 500 MB
    public string[] AllowedMimeTypes { get; init; } = [];
    public string[] AllowedExtensions { get; init; } = [];
}
