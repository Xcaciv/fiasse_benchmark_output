using Microsoft.AspNetCore.Http;
using LooseNotes.Models;

namespace LooseNotes.Services;

public interface IFileService
{
    Task<Attachment?> SaveFileAsync(IFormFile file, int noteId);
    Task<bool> DeleteFileAsync(int attachmentId);
    Task<Attachment?> GetFileAsync(int attachmentId);
    bool IsValidFile(IFormFile file);
}

public class FileService : IFileService
{
    private readonly ApplicationDbContext _context;
    private readonly IWebHostEnvironment _environment;
    private readonly ILogger<FileService> _logger;
    private readonly string[] _allowedExtensions = { ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg" };
    private readonly long _maxFileSize = 10 * 1024 * 1024; // 10MB

    public FileService(ApplicationDbContext context, IWebHostEnvironment environment, ILogger<FileService> logger)
    {
        _context = context;
        _environment = environment;
        _logger = logger;
    }

    public bool IsValidFile(IFormFile file)
    {
        if (file == null || file.Length == 0)
            return false;

        if (file.Length > _maxFileSize)
            return false;

        var extension = Path.GetExtension(file.FileName).ToLowerInvariant();
        return _allowedExtensions.Contains(extension);
    }

    public async Task<Attachment?> SaveFileAsync(IFormFile file, int noteId)
    {
        if (!IsValidFile(file))
            return null;

        var uploadPath = Path.Combine(_environment.ContentRootPath, "wwwroot", "uploads");
        if (!Directory.Exists(uploadPath))
            Directory.CreateDirectory(uploadPath);

        var extension = Path.GetExtension(file.FileName).ToLowerInvariant();
        var storedFileName = $"{Guid.NewGuid()}{extension}";
        var filePath = Path.Combine(uploadPath, storedFileName);

        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await file.CopyToAsync(stream);
        }

        var attachment = new Attachment
        {
            FileName = file.FileName,
            StoredFileName = storedFileName,
            ContentType = file.ContentType,
            FileSize = file.Length,
            NoteId = noteId,
            UploadedAt = DateTime.UtcNow
        };

        _context.Attachments.Add(attachment);
        await _context.SaveChangesAsync();

        _logger.LogInformation("File saved: {FileName} for note {NoteId}", file.FileName, noteId);

        return attachment;
    }

    public async Task<bool> DeleteFileAsync(int attachmentId)
    {
        var attachment = await _context.Attachments.FindAsync(attachmentId);
        if (attachment == null)
            return false;

        var filePath = Path.Combine(_environment.ContentRootPath, "wwwroot", "uploads", attachment.StoredFileName);
        if (File.Exists(filePath))
        {
            File.Delete(filePath);
        }

        _context.Attachments.Remove(attachment);
        await _context.SaveChangesAsync();

        _logger.LogInformation("File deleted: {FileName}", attachment.FileName);

        return true;
    }

    public async Task<Attachment?> GetFileAsync(int attachmentId)
    {
        return await _context.Attachments.FindAsync(attachmentId);
    }
}
