namespace LooseNotes.Services;

public interface IFileStorageService
{
    Task<string> SaveFileAsync(IFormFile file, string uniqueFileName);
    Task DeleteFileAsync(string storedFileName);
    string GetFilePath(string storedFileName);
}
