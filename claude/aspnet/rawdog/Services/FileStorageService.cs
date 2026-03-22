namespace LooseNotes.Services;

public class FileStorageService : IFileStorageService
{
    private readonly string _uploadPath;
    private readonly long _maxFileSizeBytes;
    private readonly string[] _allowedExtensions;
    private readonly IWebHostEnvironment _env;
    private readonly ILogger<FileStorageService> _logger;

    public FileStorageService(IConfiguration config, IWebHostEnvironment env, ILogger<FileStorageService> logger)
    {
        _env = env;
        _logger = logger;
        _maxFileSizeBytes = config.GetValue<long>("FileStorage:MaxFileSizeBytes", 10_485_760);
        _allowedExtensions = config.GetSection("FileStorage:AllowedExtensions").Get<string[]>()
            ?? new[] { ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg" };

        var uploadRelPath = config.GetValue<string>("FileStorage:UploadPath", "wwwroot/uploads")!;
        _uploadPath = Path.Combine(_env.ContentRootPath, uploadRelPath);
        Directory.CreateDirectory(_uploadPath);
    }

    public async Task<(string storedFileName, string contentType)> SaveFileAsync(IFormFile file)
    {
        var ext = Path.GetExtension(file.FileName).ToLowerInvariant();
        var storedFileName = $"{Guid.NewGuid()}{ext}";
        var fullPath = Path.Combine(_uploadPath, storedFileName);

        await using var stream = new FileStream(fullPath, FileMode.Create);
        await file.CopyToAsync(stream);

        _logger.LogInformation("File saved: {StoredFileName} (original: {OriginalFileName})", storedFileName, file.FileName);
        return (storedFileName, file.ContentType);
    }

    public void DeleteFile(string storedFileName)
    {
        var fullPath = Path.Combine(_uploadPath, storedFileName);
        if (File.Exists(fullPath))
        {
            File.Delete(fullPath);
            _logger.LogInformation("File deleted: {StoredFileName}", storedFileName);
        }
    }

    public string GetFilePath(string storedFileName)
        => Path.Combine(_uploadPath, storedFileName);

    public bool IsAllowedExtension(string fileName)
    {
        var ext = Path.GetExtension(fileName).ToLowerInvariant();
        return _allowedExtensions.Contains(ext);
    }

    public bool IsWithinSizeLimit(long fileSize) => fileSize <= _maxFileSizeBytes;
}
