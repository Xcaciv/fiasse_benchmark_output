using LooseNotes.Models;
using LooseNotes.Services.Interfaces;
using Microsoft.AspNetCore.WebUtilities;

namespace LooseNotes.Services;

/// <summary>
/// Stores uploaded files under the configured Uploads/ path outside wwwroot.
/// Enforces allowed extensions and max size at the trust boundary (Integrity + Availability).
/// </summary>
public class FileStorageService : IFileStorageService
{
    private readonly string _uploadPath;
    private readonly long _maxFileSizeBytes;
    private readonly ILogger<FileStorageService> _logger;

    public FileStorageService(IConfiguration config, ILogger<FileStorageService> logger)
    {
        ArgumentNullException.ThrowIfNull(config);
        _logger = logger;
        _uploadPath = config["FileStorage:UploadPath"] ?? "Uploads";
        _maxFileSizeBytes = config.GetValue<long>("FileStorage:MaxFileSizeBytes", Attachment.MaxFileSizeBytes);
        Directory.CreateDirectory(_uploadPath);
    }

    /// <inheritdoc />
    public async Task<(string StoredFileName, long FileSizeBytes)> StoreFileAsync(IFormFile file)
    {
        ArgumentNullException.ThrowIfNull(file);
        ValidateFile(file);

        var extension = Path.GetExtension(file.FileName).ToLowerInvariant();
        var storedName = $"{Guid.NewGuid():N}{extension}";
        var fullPath = Path.Combine(_uploadPath, storedName);

        await using var stream = new FileStream(fullPath, FileMode.CreateNew, FileAccess.Write);
        await file.CopyToAsync(stream);

        _logger.LogInformation("File stored as {StoredName}, size {Size} bytes", storedName, file.Length);
        return (storedName, file.Length);
    }

    /// <inheritdoc />
    public Task<bool> DeleteFileAsync(string storedFileName)
    {
        ArgumentNullException.ThrowIfNull(storedFileName);
        var fullPath = Path.Combine(_uploadPath, storedFileName);

        if (!File.Exists(fullPath))
        {
            return Task.FromResult(false);
        }

        try
        {
            File.Delete(fullPath);
            return Task.FromResult(true);
        }
        catch (IOException ex)
        {
            _logger.LogError(ex, "Failed to delete file {StoredName}", storedFileName);
            return Task.FromResult(false);
        }
    }

    /// <inheritdoc />
    public string GetFilePath(string storedFileName)
    {
        ArgumentNullException.ThrowIfNull(storedFileName);
        return Path.Combine(_uploadPath, storedFileName);
    }

    /// <inheritdoc />
    public bool IsAllowedFileType(string fileName)
    {
        if (string.IsNullOrWhiteSpace(fileName)) return false;
        var ext = Path.GetExtension(fileName).ToLowerInvariant();
        return Attachment.AllowedExtensions.Contains(ext);
    }

    private void ValidateFile(IFormFile file)
    {
        if (!IsAllowedFileType(file.FileName))
        {
            throw new ArgumentException($"File type not allowed: {Path.GetExtension(file.FileName)}");
        }

        if (file.Length > _maxFileSizeBytes)
        {
            throw new ArgumentException($"File exceeds maximum size of {_maxFileSizeBytes} bytes.");
        }
    }
}
