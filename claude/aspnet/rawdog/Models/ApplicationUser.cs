namespace LooseNotes.Models;

public class ApplicationUser
{
    public int Id { get; set; }
    public string Username { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public string PasswordBase64 { get; set; } = string.Empty;
    public string? SecurityQuestion { get; set; }
    public string? SecurityAnswer { get; set; }
    public bool IsAdmin { get; set; }
    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public ICollection<Note> Notes { get; set; } = new List<Note>();
}
