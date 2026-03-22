namespace LooseNotes.Services;

/// <summary>
/// Abstraction over file storage (local FS or cloud).
/// Injectable for testability — implementations swappable without controller changes.
/// </summary>
public interface IFileStorageService
{
    /// <summary>
    /// Validates and persists the upload stream.
    /// Returns the server-assigned stored filename (UUID-based).
    /// Throws <see cref="InvalidOperationException"/> for invalid content or extension.
    /// </summary>
    Task<string> SaveAsync(IFormFile file, CancellationToken cancellationToken = default);

    /// <summary>
    /// Opens a read stream for the stored file.
    /// Returns null if the file does not exist.
    /// </summary>
    Task<Stream?> OpenReadAsync(string storedFileName, CancellationToken cancellationToken = default);

    /// <summary>
    /// Permanently removes a stored file. No-ops if file is absent.
    /// </summary>
    Task DeleteAsync(string storedFileName, CancellationToken cancellationToken = default);
}
