namespace LooseNotes.ViewModels.Notes;

public sealed class RatingDisplayViewModel
{
    public string UserName { get; set; } = string.Empty;
    public int Value { get; set; }
    public string? Comment { get; set; }
    public DateTime CreatedAtUtc { get; set; }
    public bool IsCurrentUsersRating { get; set; }
}
