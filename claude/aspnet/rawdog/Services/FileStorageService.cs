namespace LooseNotes.Services;

public class FileStorageService : IFileStorageService
{
    private readonly string _uploadPath;
    private readonly ILogger<FileStorageService> _logger;

    public FileStorageService(IConfiguration configuration, ILogger<FileStorageService> logger)
    {
        _logger = logger;
        _uploadPath = configuration["FileStorage:UploadPath"] ?? Path.Combine(Directory.GetCurrentDirectory(), "wwwroot", "uploads");
        Directory.CreateDirectory(_uploadPath);
    }

    public async Task<string> SaveFileAsync(IFormFile file, string uniqueFileName)
    {
        var filePath = Path.Combine(_uploadPath, uniqueFileName);
        await using var stream = new FileStream(filePath, FileMode.Create);
        await file.CopyToAsync(stream);
        _logger.LogInformation("File saved: {FileName}", uniqueFileName);
        return uniqueFileName;
    }

    public Task DeleteFileAsync(string storedFileName)
    {
        var filePath = Path.Combine(_uploadPath, storedFileName);
        if (File.Exists(filePath))
        {
            File.Delete(filePath);
            _logger.LogInformation("File deleted: {FileName}", storedFileName);
        }
        return Task.CompletedTask;
    }

    public string GetFilePath(string storedFileName)
    {
        return Path.Combine(_uploadPath, storedFileName);
    }
}
