namespace LooseNotes.Services;

public class FileStorageService : IFileStorageService
{
    private readonly string _uploadPath;

    public FileStorageService(IConfiguration config, IWebHostEnvironment env)
    {
        var configured = config["FileStorage:Path"];
        _uploadPath = string.IsNullOrEmpty(configured)
            ? Path.Combine(env.ContentRootPath, "uploads")
            : configured;

        Directory.CreateDirectory(_uploadPath);
    }

    public async Task<string> SaveFileAsync(IFormFile file)
    {
        var ext = Path.GetExtension(file.FileName);
        var stored = Guid.NewGuid().ToString("N") + ext;
        var fullPath = Path.Combine(_uploadPath, stored);
        using var stream = File.Create(fullPath);
        await file.CopyToAsync(stream);
        return stored;
    }

    public void DeleteFile(string storedFileName)
    {
        var fullPath = Path.Combine(_uploadPath, storedFileName);
        if (File.Exists(fullPath))
            File.Delete(fullPath);
    }

    public string GetFilePath(string storedFileName)
        => Path.Combine(_uploadPath, storedFileName);
}
