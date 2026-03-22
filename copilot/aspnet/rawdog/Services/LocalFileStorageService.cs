using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Options;
using rawdog.Options;

namespace rawdog.Services;

public sealed class LocalFileStorageService(IWebHostEnvironment environment, IOptions<FileStorageOptions> options)
    : IFileStorageService
{
    private static readonly HashSet<string> AllowedExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        ".pdf",
        ".doc",
        ".docx",
        ".txt",
        ".png",
        ".jpg",
        ".jpeg"
    };

    private readonly FileStorageOptions _options = options.Value;

    public async Task<IReadOnlyList<StoredFileResult>> SaveFilesAsync(IEnumerable<IFormFile> files, CancellationToken cancellationToken = default)
    {
        var rootPath = GetRootPath();
        Directory.CreateDirectory(rootPath);

        var storedFiles = new List<StoredFileResult>();
        foreach (var file in files.Where(file => file.Length > 0))
        {
            var extension = Path.GetExtension(file.FileName);
            if (!AllowedExtensions.Contains(extension))
            {
                throw new InvalidOperationException($"The file '{file.FileName}' is not a supported attachment type.");
            }

            if (file.Length > _options.MaxFileSizeBytes)
            {
                throw new InvalidOperationException($"The file '{file.FileName}' exceeds the maximum allowed size.");
            }

            var storedFileName = $"{Guid.NewGuid():N}{extension}";
            var absolutePath = Path.Combine(rootPath, storedFileName);

            await using var targetStream = File.Create(absolutePath);
            await file.CopyToAsync(targetStream, cancellationToken);

            storedFiles.Add(new StoredFileResult(
                storedFileName,
                Path.GetFileName(file.FileName),
                string.IsNullOrWhiteSpace(file.ContentType) ? "application/octet-stream" : file.ContentType,
                file.Length));
        }

        return storedFiles;
    }

    public Task DeleteFileAsync(string storedFileName, CancellationToken cancellationToken = default)
    {
        cancellationToken.ThrowIfCancellationRequested();

        var absolutePath = GetAbsolutePath(storedFileName);
        if (File.Exists(absolutePath))
        {
            File.Delete(absolutePath);
        }

        return Task.CompletedTask;
    }

    public string GetAbsolutePath(string storedFileName)
    {
        return Path.Combine(GetRootPath(), storedFileName);
    }

    private string GetRootPath()
    {
        return Path.Combine(environment.ContentRootPath, _options.RootPath);
    }
}
