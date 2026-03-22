using LooseNotes.Models;

namespace LooseNotes.ViewModels.Notes;

public class NoteDetailsViewModel
{
    public Note Note { get; set; } = null!;
    public bool IsOwner { get; set; }
    public bool IsAdmin { get; set; }
    public double AverageRating { get; set; }
    public int RatingCount { get; set; }
    public Rating? CurrentUserRating { get; set; }
    public string? ShareToken { get; set; }
}
