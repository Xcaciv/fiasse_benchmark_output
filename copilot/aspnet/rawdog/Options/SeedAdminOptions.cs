namespace rawdog.Options;

public sealed class SeedAdminOptions
{
    public const string SectionName = "SeedAdmin";

    public string UserName { get; set; } = "admin";

    public string Email { get; set; } = "admin@loosenotes.local";

    public string Password { get; set; } = "Admin123!";
}
