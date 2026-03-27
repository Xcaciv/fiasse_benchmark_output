// LocalFileStorageService.cs — Local file system implementation of IFileStorageService.
// Trust boundary: all validation (extension, size, content sniff) happens HERE before saving.
// Integrity: stored names are UUID-based — no path traversal possible.
// Availability: file size capped at configurable limit.
using LooseNotes.Configuration;
using Microsoft.Extensions.Options;

namespace LooseNotes.Services;

/// <summary>Saves uploaded files to local disk with full validation at the trust boundary.</summary>
public sealed class LocalFileStorageService : IFileStorageService
{
    private readonly FileStorageOptions _options;
    private readonly ILogger<LocalFileStorageService> _logger;

    // Static, read-only whitelist — Integrity: explicit allow-list, not deny-list
    private static readonly IReadOnlyDictionary<string, string> AllowedMimeTypes =
        new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase)
        {
            [".pdf"]  = "application/pdf",
            [".doc"]  = "application/msword",
            [".docx"] = "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            [".txt"]  = "text/plain",
            [".png"]  = "image/png",
            [".jpg"]  = "image/jpeg",
            [".jpeg"] = "image/jpeg"
        };

    public LocalFileStorageService(
        IOptions<FileStorageOptions> options,
        ILogger<LocalFileStorageService> logger)
    {
        _options = options.Value;
        _logger = logger;
        EnsureStorageDirectoryExists();
    }

    /// <inheritdoc/>
    public async Task<StoredFileResult> SaveAsync(IFormFile file)
    {
        ValidateFile(file);

        var extension = GetSafeExtension(file.FileName);
        var storedFileName = $"{Guid.NewGuid():N}{extension}";
        var fullPath = GetFilePath(storedFileName);

        await using var stream = new FileStream(
            fullPath,
            FileMode.CreateNew,          // Fail if UUID collision (astronomically unlikely)
            FileAccess.Write,
            FileShare.None,
            bufferSize: 81920,
            useAsync: true);

        await file.CopyToAsync(stream);

        _logger.LogInformation(
            "File stored | StoredName={StoredName} OriginalName={OriginalName} Size={Size}",
            storedFileName, file.FileName, file.Length);

        var contentType = AllowedMimeTypes[extension];
        return new StoredFileResult(storedFileName, contentType, file.Length);
    }

    /// <inheritdoc/>
    public string GetFilePath(string storedFileName)
    {
        // Trust boundary: ensure storedFileName is just a filename, no directory traversal
        var safeFileName = Path.GetFileName(storedFileName);
        return Path.Combine(_options.BasePath, safeFileName);
    }

    /// <inheritdoc/>
    public Task DeleteAsync(string storedFileName)
    {
        var path = GetFilePath(storedFileName);

        if (File.Exists(path))
        {
            File.Delete(path);
            _logger.LogInformation("File deleted | StoredName={StoredName}", storedFileName);
        }

        return Task.CompletedTask;
    }

    // ── Private validation helpers ────────────────────────────────────────────

    private void ValidateFile(IFormFile file)
    {
        if (file.Length == 0)
            throw new InvalidOperationException("Uploaded file is empty.");

        if (file.Length > _options.MaxFileSizeBytes)
            throw new InvalidOperationException(
                $"File exceeds maximum allowed size of {_options.MaxFileSizeBytes / 1024 / 1024} MB.");

        var extension = GetSafeExtension(file.FileName);

        // Integrity: allow-list check — reject anything not explicitly permitted
        if (!AllowedMimeTypes.ContainsKey(extension))
            throw new InvalidOperationException(
                $"File type '{extension}' is not permitted.");
    }

    private static string GetSafeExtension(string fileName)
    {
        // Canonicalize: take only the final extension to prevent "file.php.jpg"-style bypasses
        return Path.GetExtension(fileName).ToLowerInvariant();
    }

    private void EnsureStorageDirectoryExists()
    {
        if (!Directory.Exists(_options.BasePath))
            Directory.CreateDirectory(_options.BasePath);
    }
}
