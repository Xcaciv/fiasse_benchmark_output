using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.Options;
using LooseNotes.Options;

namespace LooseNotes.Services;

public sealed class LocalFileStorageService : IFileStorageService
{
    private static readonly HashSet<string> InlineMimeTypes = new(StringComparer.OrdinalIgnoreCase)
    {
        "application/pdf",
        "text/plain",
        "image/png",
        "image/jpeg"
    };

    private readonly FileStorageOptions _options;
    private readonly IWebHostEnvironment _environment;

    public LocalFileStorageService(IOptions<FileStorageOptions> options, IWebHostEnvironment environment)
    {
        _options = options.Value;
        _environment = environment;
    }

    public async Task<StoredFileResult> SaveAsync(IFormFile file, CancellationToken cancellationToken = default)
    {
        if (file is null)
        {
            throw new ArgumentNullException(nameof(file));
        }

        if (file.Length <= 0)
        {
            throw new InvalidOperationException("The uploaded file is empty.");
        }

        if (file.Length > _options.MaxBytes)
        {
            throw new InvalidOperationException($"The uploaded file exceeds the {_options.MaxBytes / (1024 * 1024)} MB limit.");
        }

        var extension = Path.GetExtension(file.FileName);
        if (string.IsNullOrWhiteSpace(extension) || !_options.AllowedExtensions.Contains(extension, StringComparer.OrdinalIgnoreCase))
        {
            throw new InvalidOperationException("Unsupported file type. Allowed types are PDF, DOC, DOCX, TXT, PNG, JPG, and JPEG.");
        }

        var storageRoot = GetStorageRoot();
        Directory.CreateDirectory(storageRoot);

        var storedFileName = $"{Guid.NewGuid():N}{extension.ToLowerInvariant()}";
        var absolutePath = Path.Combine(storageRoot, storedFileName);

        await using var stream = new FileStream(absolutePath, FileMode.CreateNew, FileAccess.Write, FileShare.None);
        await file.CopyToAsync(stream, cancellationToken);

        var contentType = string.IsNullOrWhiteSpace(file.ContentType)
            ? extension.Equals(".txt", StringComparison.OrdinalIgnoreCase) ? "text/plain" : "application/octet-stream"
            : file.ContentType;

        return new StoredFileResult(storedFileName, Path.GetFileName(file.FileName), contentType, file.Length);
    }

    public Task DeleteAsync(string storedFileName, CancellationToken cancellationToken = default)
    {
        var absolutePath = GetAbsolutePath(storedFileName);
        if (File.Exists(absolutePath))
        {
            File.Delete(absolutePath);
        }

        return Task.CompletedTask;
    }

    public string GetAbsolutePath(string storedFileName)
    {
        var safeFileName = Path.GetFileName(storedFileName);
        if (!string.Equals(safeFileName, storedFileName, StringComparison.Ordinal))
        {
            throw new InvalidOperationException("Invalid file name.");
        }

        return Path.Combine(GetStorageRoot(), safeFileName);
    }

    private string GetStorageRoot()
    {
        return Path.GetFullPath(Path.Combine(_environment.ContentRootPath, _options.StorageRootPath));
    }
}
