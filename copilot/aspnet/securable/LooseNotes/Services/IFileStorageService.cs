namespace LooseNotes.Services;

/// <summary>
/// Abstracts file storage so the backing store can be swapped without touching controllers.
/// Modifiability: all file I/O is centralized here, not scattered across controllers.
/// </summary>
public interface IFileStorageService
{
    /// <summary>Validates, stores the file, and returns the unique stored file name.</summary>
    Task<string> SaveAsync(IFormFile file, CancellationToken ct = default);

    /// <summary>Deletes the stored file if it exists. Safe to call on missing files.</summary>
    Task DeleteAsync(string storedName, CancellationToken ct = default);

    /// <summary>Returns the absolute file-system path for a given stored name.</summary>
    string GetFilePath(string storedName);
}
