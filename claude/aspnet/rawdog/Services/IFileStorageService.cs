namespace LooseNotes.Services;

public interface IFileStorageService
{
    string AttachmentsBasePath { get; }
    Task<string> SaveFileAsync(IFormFile file, string fileName);
}
