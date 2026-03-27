namespace LooseNotes.Services;

public interface IFileStorageService
{
    Task<string> SaveFileAsync(IFormFile file);
    void DeleteFile(string storedFileName);
    string GetFilePath(string storedFileName);
}
