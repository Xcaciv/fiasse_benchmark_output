using Microsoft.AspNetCore.Http;

namespace LooseNotes.Services;

public interface IFileStorageService
{
    Task<StoredFileResult> SaveAsync(IFormFile file, CancellationToken cancellationToken = default);
    Task DeleteAsync(string storedFileName, CancellationToken cancellationToken = default);
    string GetAbsolutePath(string storedFileName);
}

public sealed record StoredFileResult(string StoredFileName, string OriginalFileName, string ContentType, long SizeBytes);
