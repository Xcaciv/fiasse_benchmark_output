namespace LooseNotes.Configuration;

/// <summary>
/// Externalized password policy. Shared by registration and profile update flows
/// so policy changes apply uniformly (FIASSE S2.1, ASVS V6.2).
/// </summary>
public sealed class PasswordPolicyOptions
{
    public const string SectionName = "PasswordPolicy";

    public int MinimumLength { get; init; } = 8;
    public int RecommendedLength { get; init; } = 15;
    public int MaximumLength { get; init; } = 128;
    public string CommonPasswordsResourceName { get; init; } = "LooseNotes.Resources.CommonPasswords.txt";
}
