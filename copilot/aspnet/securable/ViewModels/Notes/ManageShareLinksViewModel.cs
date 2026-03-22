namespace LooseNotes.ViewModels.Notes;

public sealed class ManageShareLinksViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public IReadOnlyList<ShareLinkDisplayViewModel> ActiveLinks { get; set; } = Array.Empty<ShareLinkDisplayViewModel>();
}
