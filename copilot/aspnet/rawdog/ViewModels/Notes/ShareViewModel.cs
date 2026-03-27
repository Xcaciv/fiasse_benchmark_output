using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public class ShareViewModel
{
    public Note Note { get; set; } = null!;
    public List<ShareLink> ShareLinks { get; set; } = new();
    public string BaseUrl { get; set; } = string.Empty;
}
