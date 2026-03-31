using Microsoft.EntityFrameworkCore;
using LooseNotes.Data;
using LooseNotes.Models;
using System.Security.Cryptography;
using Microsoft.Extensions.Configuration;

namespace LooseNotes.Services;

public class FileService : IFileService
{
    private readonly ApplicationDbContext _context;
    private readonly IWebHostEnvironment _environment;
    private readonly IConfiguration _configuration;
    private readonly ILogger<FileService> _logger;

    private static readonly HashSet<string> AllowedExtensions = new(StringComparer.OrdinalIgnoreCase)
    {
        ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg"
    };

    public FileService(
        ApplicationDbContext context,
        IWebHostEnvironment environment,
        IConfiguration configuration,
        ILogger<FileService> logger)
    {
        _context = context;
        _environment = environment;
        _configuration = configuration;
        _logger = logger;
    }

    public async Task<Attachment> SaveFileAsync(IFormFile file, int noteId)
    {
        var storagePath = _configuration["FileUpload:StoragePath"] ?? "wwwroot/uploads";
        var fullPath = Path.Combine(_environment.ContentRootPath, storagePath);
        
        Directory.CreateDirectory(fullPath);

        var storedFileName = GenerateUniqueFileName(Path.GetExtension(file.FileName));
        var filePath = Path.Combine(fullPath, storedFileName);

        using (var stream = new FileStream(filePath, FileMode.Create))
        {
            await file.CopyToAsync(stream);
        }

        var attachment = new Attachment
        {
            NoteId = noteId,
            FileName = file.FileName,
            StoredFileName = storedFileName,
            ContentType = file.ContentType,
            FileSize = file.Length,
            UploadedAt = DateTime.UtcNow
        };

        _context.Attachments.Add(attachment);
        await _context.SaveChangesAsync();
        
        _logger.LogInformation("File uploaded: {FileName} for note {NoteId}", file.FileName, noteId);
        
        return attachment;
    }

    public async Task<byte[]> GetFileAsync(string storedFileName)
    {
        var storagePath = _configuration["FileUpload:StoragePath"] ?? "wwwroot/uploads";
        var fullPath = Path.Combine(_environment.ContentRootPath, storagePath, storedFileName);
        
        if (!File.Exists(fullPath))
        {
            throw new FileNotFoundException("File not found", storedFileName);
        }

        return await File.ReadAllBytesAsync(fullPath);
    }

    public async Task<bool> DeleteFileAsync(string storedFileName)
    {
        var attachment = await _context.Attachments
            .FirstOrDefaultAsync(a => a.StoredFileName == storedFileName);

        if (attachment == null)
        {
            return false;
        }

        var storagePath = _configuration["FileUpload:StoragePath"] ?? "wwwroot/uploads";
        var fullPath = Path.Combine(_environment.ContentRootPath, storagePath, storedFileName);

        if (File.Exists(fullPath))
        {
            File.Delete(fullPath);
        }

        _context.Attachments.Remove(attachment);
        await _context.SaveChangesAsync();
        
        _logger.LogInformation("File deleted: {FileName}", storedFileName);
        
        return true;
    }

    public bool IsValidFileType(string fileName)
    {
        var extension = Path.GetExtension(fileName);
        return AllowedExtensions.Contains(extension);
    }

    public bool IsValidFileSize(long size)
    {
        var maxSize = _configuration.GetValue<long>("FileUpload:MaxFileSizeBytes", 10485760);
        return size <= maxSize;
    }

    private static string GenerateUniqueFileName(string extension)
    {
        var bytes = new byte[16];
        using var rng = RandomNumberGenerator.Create();
        rng.GetBytes(bytes);
        return $"{Guid.NewGuid():N}{extension}";
    }
}
