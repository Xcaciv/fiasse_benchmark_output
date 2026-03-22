namespace LooseNotes.Services;

/// <summary>
/// Stores uploaded files in a configurable directory on the local file system.
///
/// SSEM / FIASSE security measures applied:
///  - Stored filename is always a GUID – never derived from user-supplied input.
///  - Allowed extensions are white-listed from configuration (deny by default).
///  - File size is bounded by configuration.
///  - Upload directory is created if it doesn't exist; not inside wwwroot (not web-accessible directly).
///  - Path.Combine + GetFullPath + StartsWith guards against directory traversal.
/// </summary>
public class LocalFileStorageService : IFileStorageService
{
    private readonly string _uploadRoot;
    private readonly long _maxFileSizeBytes;
    private readonly HashSet<string> _allowedExtensions;
    private readonly ILogger<LocalFileStorageService> _logger;

    public LocalFileStorageService(IConfiguration config, IWebHostEnvironment env,
        ILogger<LocalFileStorageService> logger)
    {
        _logger = logger;

        var uploadPath = config["FileStorage:UploadPath"] ?? "uploads";
        // Store uploads outside wwwroot to prevent direct web access
        _uploadRoot = Path.IsPathRooted(uploadPath)
            ? uploadPath
            : Path.Combine(env.ContentRootPath, uploadPath);

        _maxFileSizeBytes = config.GetValue<long>("FileStorage:MaxFileSizeBytes", 10_485_760); // 10 MB

        var extensions = config.GetSection("FileStorage:AllowedExtensions").Get<string[]>()
            ?? new[] { ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg" };
        _allowedExtensions = new HashSet<string>(extensions, StringComparer.OrdinalIgnoreCase);

        Directory.CreateDirectory(_uploadRoot);
    }

    public async Task<string> SaveAsync(IFormFile file)
    {
        // Validate extension (white-list)
        var ext = Path.GetExtension(file.FileName);
        if (string.IsNullOrEmpty(ext) || !_allowedExtensions.Contains(ext))
            throw new InvalidOperationException($"File type '{ext}' is not allowed.");

        // Validate size
        if (file.Length > _maxFileSizeBytes)
            throw new InvalidOperationException(
                $"File exceeds the maximum allowed size of {_maxFileSizeBytes / 1_048_576} MB.");

        // Generate a collision-free stored name (never user-supplied)
        var storedName = $"{Guid.NewGuid():N}{ext.ToLowerInvariant()}";
        var destPath = BuildSafePath(storedName);

        await using var dest = new FileStream(destPath, FileMode.CreateNew, FileAccess.Write);
        await file.CopyToAsync(dest);

        _logger.LogInformation("Stored uploaded file as {StoredName} (original: {OriginalName}, {Bytes} bytes)",
            storedName, file.FileName, file.Length);

        return storedName;
    }

    public string? GetPhysicalPath(string storedFileName)
    {
        // Defensive: ensure the resolved path stays within the upload root
        var path = BuildSafePath(storedFileName);
        return File.Exists(path) ? path : null;
    }

    public Task DeleteAsync(string storedFileName)
    {
        var path = BuildSafePath(storedFileName);
        if (File.Exists(path))
        {
            File.Delete(path);
            _logger.LogInformation("Deleted stored file {StoredName}", storedFileName);
        }
        return Task.CompletedTask;
    }

    /// <summary>
    /// Builds an absolute path and validates it is inside the upload root.
    /// Throws if the resolved path escapes the root (directory traversal guard).
    /// </summary>
    private string BuildSafePath(string storedFileName)
    {
        // storedFileName should be a GUID-based name; strip any path components
        var safeName = Path.GetFileName(storedFileName);
        if (string.IsNullOrEmpty(safeName))
            throw new InvalidOperationException("Invalid stored filename.");

        var full = Path.GetFullPath(Path.Combine(_uploadRoot, safeName));
        if (!full.StartsWith(Path.GetFullPath(_uploadRoot), StringComparison.OrdinalIgnoreCase))
            throw new InvalidOperationException("Attempted path traversal detected.");

        return full;
    }
}
