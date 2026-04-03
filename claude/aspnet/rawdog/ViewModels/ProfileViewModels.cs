namespace LooseNotes.ViewModels;

public class ProfileEditViewModel
{
    public int UserId { get; set; }
    public string Username { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string? PasswordBase64 { get; set; }
    public string? NewPassword { get; set; }
    public string? ConfirmNewPassword { get; set; }
    public string? SecurityQuestion { get; set; }
    public string? SecurityAnswer { get; set; }
}
