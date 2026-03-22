namespace LooseNotes.Options;

public sealed class FileStorageOptions
{
    public string StorageRootPath { get; set; } = Path.Combine("App_Data", "attachments");
    public long MaxBytes { get; set; } = 10 * 1024 * 1024;
    public IList<string> AllowedExtensions { get; set; } = new List<string>
    {
        ".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg"
    };
}
