using Microsoft.AspNetCore.Http;

namespace LooseNotes.Services.Interfaces;

/// <summary>
/// Abstracts file I/O behind an interface so storage backend is replaceable (Modifiability).
/// Enforces allowed extensions and max size (Availability + Integrity).
/// </summary>
public interface IFileStorageService
{
    /// <summary>
    /// Persists file to storage and returns a tuple of the stored filename and byte count.
    /// Throws ArgumentException if file type not allowed or size exceeds limit.
    /// </summary>
    Task<(string StoredFileName, long FileSizeBytes)> StoreFileAsync(IFormFile file);

    /// <summary>Deletes stored file. Returns false if file not found.</summary>
    Task<bool> DeleteFileAsync(string storedFileName);

    /// <summary>Returns absolute disk path for serving.</summary>
    string GetFilePath(string storedFileName);

    /// <summary>Returns true if file extension is in the allowed list.</summary>
    bool IsAllowedFileType(string fileName);
}
