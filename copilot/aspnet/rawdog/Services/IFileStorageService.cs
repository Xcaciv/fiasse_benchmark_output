using Microsoft.AspNetCore.Http;

namespace rawdog.Services;

public interface IFileStorageService
{
    Task<IReadOnlyList<StoredFileResult>> SaveFilesAsync(IEnumerable<IFormFile> files, CancellationToken cancellationToken = default);

    Task DeleteFileAsync(string storedFileName, CancellationToken cancellationToken = default);

    string GetAbsolutePath(string storedFileName);
}
