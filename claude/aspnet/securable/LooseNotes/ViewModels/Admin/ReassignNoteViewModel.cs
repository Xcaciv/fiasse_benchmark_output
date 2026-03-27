// ReassignNoteViewModel.cs — Input for admin note ownership reassignment.
// Integrity: NoteId and TargetUserId are validated in the controller before any DB change.
using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Admin;

public sealed class ReassignNoteViewModel
{
    public int NoteId { get; set; }
    public string NoteTitle { get; set; } = string.Empty;
    public string CurrentOwnerUserName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Please select a new owner.")]
    public string TargetUserId { get; set; } = string.Empty;

    /// <summary>All users available as reassignment targets (populated by controller).</summary>
    public IReadOnlyList<UserOptionViewModel> AvailableUsers { get; set; } = Array.Empty<UserOptionViewModel>();
}

public sealed class UserOptionViewModel
{
    public string Id { get; set; } = string.Empty;
    public string UserName { get; set; } = string.Empty;
}
