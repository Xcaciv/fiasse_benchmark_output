// IFileStorageService.cs — Abstraction for file upload and retrieval.
// Modifiability: swap local storage for S3/Azure Blob without changing controllers.
namespace LooseNotes.Services;

/// <summary>Result of a file save operation.</summary>
/// <param name="StoredFileName">UUID-based name used on the file system.</param>
/// <param name="ContentType">Detected/validated MIME type.</param>
/// <param name="FileSizeBytes">Persisted file size.</param>
public sealed record StoredFileResult(
    string StoredFileName,
    string ContentType,
    long FileSizeBytes);

/// <summary>Stores and retrieves uploaded attachments with validation.</summary>
public interface IFileStorageService
{
    /// <summary>Validates and persists an uploaded file.
    /// Throws <see cref="InvalidOperationException"/> if validation fails.</summary>
    Task<StoredFileResult> SaveAsync(IFormFile file);

    /// <summary>Returns the absolute file-system path for a stored file.
    /// Callers must not pass user-supplied values as storedFileName.</summary>
    string GetFilePath(string storedFileName);

    /// <summary>Deletes a stored file. No-ops if the file does not exist.</summary>
    Task DeleteAsync(string storedFileName);
}
