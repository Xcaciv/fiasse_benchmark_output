using Microsoft.AspNetCore.Http;

namespace LooseNotes.Services;

public class FileStorageService : IFileStorageService
{
    private static readonly HashSet<string> AllowedExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg"
    };

    private static readonly Dictionary<string, string> ContentTypes = new(StringComparer.OrdinalIgnoreCase)
    {
        { ".pdf", "application/pdf" },
        { ".doc", "application/msword" },
        { ".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document" },
        { ".txt", "text/plain" },
        { ".png", "image/png" },
        { ".jpg", "image/jpeg" },
        { ".jpeg", "image/jpeg" }
    };

    private const long MaxFileSizeBytes = 10 * 1024 * 1024; // 10 MB

    private readonly string _storagePath;
    private readonly ILogger<FileStorageService> _logger;

    public FileStorageService(IConfiguration configuration, ILogger<FileStorageService> logger)
    {
        _storagePath = configuration["FileStorage:Path"] ?? Path.Combine(Directory.GetCurrentDirectory(), "uploads");
        _logger = logger;

        if (!Directory.Exists(_storagePath))
        {
            Directory.CreateDirectory(_storagePath);
        }
    }

    public bool IsAllowedExtension(string fileName)
    {
        var ext = Path.GetExtension(fileName);
        return !string.IsNullOrEmpty(ext) && AllowedExtensions.Contains(ext);
    }

    public bool IsWithinSizeLimit(long fileSize) => fileSize <= MaxFileSizeBytes;

    public async Task<(string storedFileName, string contentType)> SaveFileAsync(
        IFormFile file,
        CancellationToken cancellationToken = default)
    {
        var extension = Path.GetExtension(file.FileName);
        if (!IsAllowedExtension(file.FileName))
        {
            throw new InvalidOperationException($"File extension '{extension}' is not allowed.");
        }

        if (!IsWithinSizeLimit(file.Length))
        {
            throw new InvalidOperationException("File exceeds the 10 MB size limit.");
        }

        var storedFileName = $"{Guid.NewGuid()}{extension}";
        var filePath = Path.Combine(_storagePath, storedFileName);
        var contentType = ContentTypes.TryGetValue(extension, out var ct) ? ct : "application/octet-stream";

        try
        {
            using var stream = new FileStream(filePath, FileMode.CreateNew, FileAccess.Write, FileShare.None);
            await file.CopyToAsync(stream, cancellationToken);
        }
        catch (IOException ex)
        {
            _logger.LogError(ex, "IO error saving file to storage.");
            throw;
        }

        _logger.LogInformation("File saved: {StoredFileName}, ContentType: {ContentType}", storedFileName, contentType);
        return (storedFileName, contentType);
    }

    public async Task DeleteFileAsync(string storedFileName, CancellationToken cancellationToken = default)
    {
        var safeName = Path.GetFileName(storedFileName);
        var filePath = Path.Combine(_storagePath, safeName);

        if (!File.Exists(filePath))
        {
            _logger.LogWarning("DeleteFile: file not found: {StoredFileName}", storedFileName);
            return;
        }

        try
        {
            await Task.Run(() => File.Delete(filePath), cancellationToken);
            _logger.LogInformation("File deleted: {StoredFileName}", storedFileName);
        }
        catch (IOException ex)
        {
            _logger.LogError(ex, "IO error deleting file: {StoredFileName}", storedFileName);
            throw;
        }
    }

    public Task<(Stream stream, string contentType, string fileName)> GetFileAsync(
        string storedFileName,
        string originalFileName,
        CancellationToken cancellationToken = default)
    {
        var safeName = Path.GetFileName(storedFileName);
        var filePath = Path.Combine(_storagePath, safeName);

        if (!File.Exists(filePath))
        {
            throw new FileNotFoundException("Stored file not found.", storedFileName);
        }

        var extension = Path.GetExtension(storedFileName);
        var contentType = ContentTypes.TryGetValue(extension, out var ct) ? ct : "application/octet-stream";

        Stream stream = new FileStream(filePath, FileMode.Open, FileAccess.Read, FileShare.Read);
        return Task.FromResult((stream, contentType, originalFileName));
    }
}
