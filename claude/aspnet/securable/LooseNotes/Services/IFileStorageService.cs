namespace LooseNotes.Services;

public interface IFileStorageService
{
    /// <summary>
    /// Validates and stores the uploaded file.
    /// Returns the generated stored filename (GUID-based, never user-supplied).
    /// Throws InvalidOperationException if the file is rejected.
    /// </summary>
    Task<string> SaveAsync(IFormFile file);

    /// <summary>
    /// Returns the absolute path for a stored file, or null if the file doesn't exist.
    /// SSEM: Path is constructed from config root + stored name only – no user input in path.
    /// </summary>
    string? GetPhysicalPath(string storedFileName);

    /// <summary>Deletes a stored file by its stored filename.</summary>
    Task DeleteAsync(string storedFileName);
}
