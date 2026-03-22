namespace LooseNotes.ViewModels.Notes;

public sealed class AttachmentDisplayViewModel
{
    public int Id { get; set; }
    public string OriginalFileName { get; set; } = string.Empty;
    public long SizeBytes { get; set; }
}
