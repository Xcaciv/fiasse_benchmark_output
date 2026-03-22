using LooseNotes.Configuration;
using Microsoft.Extensions.Options;

namespace LooseNotes.Services;

/// <summary>
/// Local file-system implementation of <see cref="IFileStorageService"/>.
/// Trust boundary: validates extension and size before writing.
/// Stored filenames are UUIDs — no client-supplied path component ever reaches disk (Integrity).
/// </summary>
public class LocalFileStorageService : IFileStorageService
{
    private readonly FileStorageOptions _options;
    private readonly ILogger<LocalFileStorageService> _logger;

    public LocalFileStorageService(
        IOptions<FileStorageOptions> options,
        ILogger<LocalFileStorageService> logger)
    {
        _options = options.Value;
        _logger = logger;
        EnsureUploadDirectoryExists();
    }

    public async Task<string> SaveAsync(IFormFile file, CancellationToken cancellationToken = default)
    {
        ValidateFile(file);

        var storedName = $"{Guid.NewGuid():N}{Path.GetExtension(file.FileName).ToLowerInvariant()}";
        var fullPath = BuildSafePath(storedName);

        await using var destination = new FileStream(fullPath, FileMode.CreateNew, FileAccess.Write, FileShare.None);
        await file.CopyToAsync(destination, cancellationToken);

        _logger.LogInformation("Stored attachment storedName={StoredName} size={Size}", storedName, file.Length);
        return storedName;
    }

    public Task<Stream?> OpenReadAsync(string storedFileName, CancellationToken cancellationToken = default)
    {
        // Reject any path traversal attempts (Integrity, hard shell)
        var safeName = Path.GetFileName(storedFileName);
        if (string.IsNullOrWhiteSpace(safeName) || safeName != storedFileName)
            return Task.FromResult<Stream?>(null);

        var fullPath = BuildSafePath(safeName);
        if (!File.Exists(fullPath))
            return Task.FromResult<Stream?>(null);

        Stream stream = new FileStream(fullPath, FileMode.Open, FileAccess.Read, FileShare.Read);
        return Task.FromResult<Stream?>(stream);
    }

    public Task DeleteAsync(string storedFileName, CancellationToken cancellationToken = default)
    {
        var safeName = Path.GetFileName(storedFileName);
        if (string.IsNullOrWhiteSpace(safeName))
            return Task.CompletedTask;

        var fullPath = BuildSafePath(safeName);
        if (File.Exists(fullPath))
            File.Delete(fullPath);

        return Task.CompletedTask;
    }

    // ── Private helpers ──────────────────────────────────────────────────────

    /// <summary>
    /// Validates extension (allow-list) and size before accepting the file (Integrity).
    /// Trust boundary enforcement: canonicalize extension → validate against allow-list.
    /// </summary>
    private void ValidateFile(IFormFile file)
    {
        if (file.Length == 0)
            throw new InvalidOperationException("File is empty.");

        if (file.Length > _options.MaxFileSizeBytes)
            throw new InvalidOperationException($"File exceeds maximum size of {_options.MaxFileSizeBytes / 1048576} MB.");

        // Canonicalize: extract extension from filename, lowercase
        var extension = Path.GetExtension(file.FileName)?.ToLowerInvariant();
        if (string.IsNullOrWhiteSpace(extension) || !_options.AllowedExtensions.Contains(extension))
            throw new InvalidOperationException($"File type '{extension}' is not permitted.");
    }

    private string BuildSafePath(string storedName)
        => Path.Combine(_options.UploadPath, storedName);

    private void EnsureUploadDirectoryExists()
    {
        if (!Directory.Exists(_options.UploadPath))
            Directory.CreateDirectory(_options.UploadPath);
    }
}
