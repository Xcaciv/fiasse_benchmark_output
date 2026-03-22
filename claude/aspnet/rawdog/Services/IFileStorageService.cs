namespace LooseNotes.Services;

public interface IFileStorageService
{
    Task<(string storedFileName, string contentType)> SaveFileAsync(IFormFile file);
    void DeleteFile(string storedFileName);
    string GetFilePath(string storedFileName);
    bool IsAllowedExtension(string fileName);
    bool IsWithinSizeLimit(long fileSize);
}
