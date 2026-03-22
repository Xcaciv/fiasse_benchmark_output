namespace LooseNotes.ViewModels.Notes;

public sealed class ShareLinkDisplayViewModel
{
    public int Id { get; set; }
    public string Url { get; set; } = string.Empty;
    public DateTime CreatedAtUtc { get; set; }
}
