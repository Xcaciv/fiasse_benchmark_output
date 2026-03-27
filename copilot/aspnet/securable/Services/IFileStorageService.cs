using Microsoft.AspNetCore.Http;

namespace LooseNotes.Services;

public interface IFileStorageService
{
    Task<(string storedFileName, string contentType)> SaveFileAsync(IFormFile file, CancellationToken cancellationToken = default);
    Task DeleteFileAsync(string storedFileName, CancellationToken cancellationToken = default);
    Task<(Stream stream, string contentType, string fileName)> GetFileAsync(string storedFileName, string originalFileName, CancellationToken cancellationToken = default);
    bool IsAllowedExtension(string fileName);
    bool IsWithinSizeLimit(long fileSize);
}
