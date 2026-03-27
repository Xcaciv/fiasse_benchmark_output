namespace LooseNotes.Services;

/// <summary>
/// Local-disk file storage with allowlist validation.
/// Trust boundary: all validation occurs before any I/O.
/// </summary>
public class LocalFileStorageService : IFileStorageService
{
    // Integrity: explicit allowlists for both extension and MIME type
    private static readonly HashSet<string> AllowedExtensions =
        new(StringComparer.OrdinalIgnoreCase) { ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg" };

    private static readonly HashSet<string> AllowedContentTypes =
        new(StringComparer.OrdinalIgnoreCase)
        {
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "text/plain",
            "image/png",
            "image/jpeg"
        };

    private readonly string _basePath;
    private readonly long _maxFileSizeBytes;
    private readonly ILogger<LocalFileStorageService> _logger;

    public LocalFileStorageService(
        IConfiguration configuration,
        IWebHostEnvironment environment,
        ILogger<LocalFileStorageService> logger)
    {
        _logger = logger;
        var configured = configuration["FileStorage:BasePath"] ?? "uploads";
        _basePath = Path.Combine(environment.ContentRootPath, configured);
        var maxMb = configuration.GetValue<int>("FileStorage:MaxFileSizeMb", 10);
        _maxFileSizeBytes = maxMb * 1024L * 1024L;
        Directory.CreateDirectory(_basePath);
    }

    public async Task<string> SaveAsync(IFormFile file, CancellationToken ct = default)
    {
        // Trust boundary: canonicalize → validate before any I/O
        ValidateFile(file);

        var extension = Path.GetExtension(file.FileName).ToLowerInvariant();
        var storedName = $"{Guid.NewGuid()}{extension}";
        var fullPath = Path.Combine(_basePath, storedName);

        await using var stream = new FileStream(
            fullPath, FileMode.Create, FileAccess.Write, FileShare.None);
        await file.CopyToAsync(stream, ct);

        _logger.LogInformation("File saved as {StoredName} ({SizeBytes} bytes)", storedName, file.Length);
        return storedName;
    }

    public Task DeleteAsync(string storedName, CancellationToken ct = default)
    {
        // Resilience: reject names with path separators to prevent traversal
        if (storedName.Contains('/') || storedName.Contains('\\') || storedName.Contains(".."))
        {
            _logger.LogWarning("Rejected delete attempt with unsafe stored name");
            return Task.CompletedTask;
        }

        var fullPath = Path.Combine(_basePath, storedName);
        if (File.Exists(fullPath))
        {
            File.Delete(fullPath);
            _logger.LogInformation("Deleted file {StoredName}", storedName);
        }
        return Task.CompletedTask;
    }

    public string GetFilePath(string storedName) => Path.Combine(_basePath, storedName);

    private void ValidateFile(IFormFile file)
    {
        if (file.Length == 0)
            throw new InvalidOperationException("File is empty.");

        if (file.Length > _maxFileSizeBytes)
            throw new InvalidOperationException(
                $"File exceeds the {_maxFileSizeBytes / (1024 * 1024)} MB limit.");

        // Analyzability: each check is a single, named step
        var extension = Path.GetExtension(file.FileName);
        if (!AllowedExtensions.Contains(extension))
            throw new InvalidOperationException($"File type '{extension}' is not permitted.");

        if (!AllowedContentTypes.Contains(file.ContentType))
            throw new InvalidOperationException($"Content type '{file.ContentType}' is not permitted.");
    }
}
