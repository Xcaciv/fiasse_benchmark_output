namespace LooseNotes.ViewModels.Admin;

public sealed class ActivityLogItemViewModel
{
    public DateTime CreatedAtUtc { get; set; }
    public string ActionType { get; set; } = string.Empty;
    public string Description { get; set; } = string.Empty;
    public string? ActorUserName { get; set; }
}
