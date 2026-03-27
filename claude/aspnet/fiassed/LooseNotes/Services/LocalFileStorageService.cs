using LooseNotes.Configuration;
using LooseNotes.Data;
using Microsoft.AspNetCore.Http;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;

namespace LooseNotes.Services;

/// <summary>
/// Local filesystem storage implementation. Files stored outside web root using
/// server-generated UUID names. User-supplied filenames are metadata only (ASVS V5.3.1, V5.3.2).
/// Magic byte validation performed before storage (ASVS V5.2.2).
/// </summary>
public sealed class LocalFileStorageService : IFileStorageService
{
    private static readonly Dictionary<string, byte[][]> MagicBytes = new()
    {
        ["application/pdf"] = [[0x25, 0x50, 0x44, 0x46]],
        ["image/png"] = [[0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]],
        ["image/jpeg"] = [[0xFF, 0xD8, 0xFF]],
        ["text/plain"] = [], // No magic bytes for text; extension + content check
        ["application/msword"] = [[0xD0, 0xCF, 0x11, 0xE0]],
        ["application/vnd.openxmlformats-officedocument.wordprocessingml.document"] = [[0x50, 0x4B, 0x03, 0x04]]
    };

    private readonly FileStorageOptions _options;
    private readonly ApplicationDbContext _db;
    private readonly ILogger<LocalFileStorageService> _logger;
    private readonly string _storageRoot;

    public LocalFileStorageService(
        IOptions<FileStorageOptions> options,
        ApplicationDbContext db,
        ILogger<LocalFileStorageService> logger,
        IWebHostEnvironment env)
    {
        _options = options.Value;
        _db = db;
        _logger = logger;
        // Storage root is outside web root to prevent direct HTTP access (ASVS V5.3.1)
        _storageRoot = Path.GetFullPath(
            Path.IsPathRooted(_options.StoragePath)
                ? _options.StoragePath
                : Path.Combine(env.ContentRootPath, _options.StoragePath));

        Directory.CreateDirectory(_storageRoot);
    }

    public async Task<StoredFileResult> StoreFileAsync(IFormFile file, string userId, int noteId)
    {
        ValidateFileSize(file);
        ValidateExtension(file.FileName);
        await ValidateMagicBytesAsync(file);
        await ValidateQuotaAsync(userId, file.Length);

        // Server-generated UUID name - user input never contributes to file path (ASVS V5.3.2)
        var extension = Path.GetExtension(file.FileName).ToLowerInvariant();
        var storedFileName = $"{Guid.NewGuid():N}{extension}";
        var filePath = GetFilePath(storedFileName);

        await using var stream = new FileStream(filePath, FileMode.CreateNew, FileAccess.Write);
        await file.CopyToAsync(stream);

        _logger.LogInformation(
            "File stored. UserId={UserId} NoteId={NoteId} StoredFile={StoredFile} Size={Size}",
            userId, noteId, storedFileName, file.Length);

        return new StoredFileResult(storedFileName, file.FileName, file.ContentType, file.Length);
    }

    public string GetFilePath(string storedFileName)
    {
        // Path construction: only use server-generated filename components
        // Path.GetFileName strips any traversal attempts as defense-in-depth
        var safeFileName = Path.GetFileName(storedFileName);
        return Path.Combine(_storageRoot, safeFileName);
    }

    public Task DeleteFileAsync(string storedFileName)
    {
        var path = GetFilePath(storedFileName);
        if (File.Exists(path))
        {
            File.Delete(path);
            _logger.LogInformation("File deleted: {StoredFile}", storedFileName);
        }
        return Task.CompletedTask;
    }

    public async Task<long> GetUserStorageBytesAsync(string userId)
    {
        return await _db.Attachments
            .Where(a => a.UserId == userId)
            .SumAsync(a => a.FileSizeBytes);
    }

    public async Task<int> GetUserAttachmentCountAsync(string userId)
    {
        return await _db.Attachments.CountAsync(a => a.UserId == userId);
    }

    private void ValidateFileSize(IFormFile file)
    {
        if (file.Length > _options.MaxFileSizeBytes)
            throw new FileValidationException(
                $"File exceeds maximum size of {_options.MaxFileSizeBytes / 1_048_576} MB.");
    }

    private void ValidateExtension(string fileName)
    {
        var ext = Path.GetExtension(fileName).ToLowerInvariant();
        if (!_options.AllowedExtensions.Contains(ext))
            throw new FileValidationException($"File type '{ext}' is not permitted.");
    }

    private async Task ValidateMagicBytesAsync(IFormFile file)
    {
        var normalizedContentType = file.ContentType.ToLowerInvariant().Split(';')[0].Trim();

        if (!_options.AllowedMimeTypes.Contains(normalizedContentType))
            throw new FileValidationException($"Content type '{normalizedContentType}' is not permitted.");

        // For types with known magic bytes, validate the file signature
        if (!MagicBytes.TryGetValue(normalizedContentType, out var signatures) || signatures.Length == 0)
            return; // No magic byte check for text/plain

        var buffer = new byte[16];
        await using var stream = file.OpenReadStream();
        var bytesRead = await stream.ReadAsync(buffer.AsMemory(0, buffer.Length));
        stream.Position = 0;

        if (!signatures.Any(sig => bytesRead >= sig.Length &&
            buffer.Take(sig.Length).SequenceEqual(sig)))
        {
            throw new FileValidationException(
                "File content does not match its declared type. Upload rejected.");
        }
    }

    private async Task ValidateQuotaAsync(string userId, long newFileSize)
    {
        var currentCount = await GetUserAttachmentCountAsync(userId);
        if (currentCount >= _options.MaxAttachmentsPerUser)
            throw new FileValidationException(
                $"Attachment limit of {_options.MaxAttachmentsPerUser} files reached.");

        var currentBytes = await GetUserStorageBytesAsync(userId);
        if (currentBytes + newFileSize > _options.MaxTotalBytesPerUser)
            throw new FileValidationException("Storage quota exceeded.");
    }
}

/// <summary>
/// Signals a file validation failure. Caught by controllers to return 400/413 responses.
/// </summary>
public sealed class FileValidationException : Exception
{
    public FileValidationException(string message) : base(message) { }
}
