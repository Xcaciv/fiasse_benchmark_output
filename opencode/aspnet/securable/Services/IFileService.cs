using LooseNotes.Models;

namespace LooseNotes.Services;

public interface IFileService
{
    Task<Attachment> SaveFileAsync(IFormFile file, int noteId);
    Task<byte[]> GetFileAsync(string storedFileName);
    Task<bool> DeleteFileAsync(string storedFileName);
    bool IsValidFileType(string fileName);
    bool IsValidFileSize(long size);
}
