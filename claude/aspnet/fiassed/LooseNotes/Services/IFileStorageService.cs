using Microsoft.AspNetCore.Http;

namespace LooseNotes.Services;

/// <summary>
/// File storage abstraction. Trust boundary: all user-supplied data is treated as untrusted
/// at this interface. Implementations are responsible for path safety and quota enforcement.
/// </summary>
public interface IFileStorageService
{
    /// <summary>
    /// Validates and stores an uploaded file. Returns the server-generated stored filename.
    /// Throws if validation fails (size, type, quota) before writing to disk.
    /// </summary>
    Task<StoredFileResult> StoreFileAsync(IFormFile file, string userId, int noteId);

    /// <summary>
    /// Returns the physical path for serving a file. Path is derived from server-generated
    /// filename only; user input never contributes to the path (ASVS V5.3.2).
    /// </summary>
    string GetFilePath(string storedFileName);

    /// <summary>
    /// Deletes a stored file by its server-generated name.
    /// </summary>
    Task DeleteFileAsync(string storedFileName);

    /// <summary>
    /// Returns total storage bytes consumed by a user.
    /// </summary>
    Task<long> GetUserStorageBytesAsync(string userId);

    /// <summary>
    /// Returns attachment count for a user.
    /// </summary>
    Task<int> GetUserAttachmentCountAsync(string userId);
}

public sealed record StoredFileResult(
    string StoredFileName,
    string OriginalFileName,
    string ContentType,
    long FileSizeBytes);
