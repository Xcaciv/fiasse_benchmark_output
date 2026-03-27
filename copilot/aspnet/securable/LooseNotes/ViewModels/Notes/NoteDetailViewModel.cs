using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public class NoteDetailViewModel
{
    public NoteViewModel Note { get; set; } = null!;
    public IList<Attachment> Attachments { get; set; } = new List<Attachment>();
    public IList<Rating> Ratings { get; set; } = new List<Rating>();
    public IList<ShareLink> ShareLinks { get; set; } = new List<ShareLink>();
    public double AverageRating { get; set; }
    public bool IsOwner { get; set; }
    public bool HasRated { get; set; }
    public int? CurrentUserRatingId { get; set; }
}
