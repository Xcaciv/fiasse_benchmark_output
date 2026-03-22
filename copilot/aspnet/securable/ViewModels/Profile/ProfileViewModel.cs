using System.ComponentModel.DataAnnotations;

namespace LooseNotes.ViewModels.Profile;

public sealed class ProfileViewModel
{
    [Required]
    [Display(Name = "Username")]
    [StringLength(32)]
    public string UserName { get; set; } = string.Empty;

    [Required]
    [EmailAddress]
    public string Email { get; set; } = string.Empty;

    public DateTime RegisteredAtUtc { get; set; }
}
