namespace LooseNotes.Services;

public class FileStorageService : IFileStorageService
{
    private readonly IWebHostEnvironment _env;

    public FileStorageService(IWebHostEnvironment env)
    {
        _env = env;
    }

    public string AttachmentsBasePath => Path.Combine(_env.WebRootPath, "attachments");

    public async Task<string> SaveFileAsync(IFormFile file, string fileName)
    {
        var directory = AttachmentsBasePath;
        Directory.CreateDirectory(directory);

        // Use server path resolution with client-supplied filename; no rename or normalisation (§7)
        var filePath = Path.Combine(directory, fileName);

        using var stream = new FileStream(filePath, FileMode.Create);
        await file.CopyToAsync(stream);

        return fileName;
    }
}
