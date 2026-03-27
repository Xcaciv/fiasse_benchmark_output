using System.Reflection;
using LooseNotes.Configuration;
using Microsoft.Extensions.Options;

namespace LooseNotes.Services;

/// <summary>
/// Validates passwords against ASVS V6.2 requirements:
/// - Minimum/maximum length (V6.2.1)
/// - Common password check against top-3000 list (V6.2.4)
/// - No character composition rules imposed (V6.2.5)
///
/// The common password list is loaded from an embedded resource once and cached.
/// Updating the policy requires only configuration changes (FIASSE S2.1).
/// </summary>
public sealed class PasswordValidationService : IPasswordValidationService
{
    private readonly PasswordPolicyOptions _policy;
    private readonly HashSet<string> _commonPasswords;

    public PasswordValidationService(IOptions<PasswordPolicyOptions> options)
    {
        _policy = options.Value;
        _commonPasswords = LoadCommonPasswords(_policy.CommonPasswordsResourceName);
    }

    public string? Validate(string password)
    {
        if (string.IsNullOrEmpty(password) || password.Length < _policy.MinimumLength)
            return $"Password must be at least {_policy.MinimumLength} characters.";

        if (password.Length > _policy.MaximumLength)
            return $"Password must not exceed {_policy.MaximumLength} characters.";

        // Check against common passwords list (ASVS V6.2.4)
        if (_commonPasswords.Contains(password.ToLowerInvariant()))
            return "This password is too common. Please choose a more unique password.";

        return null; // Valid
    }

    private static HashSet<string> LoadCommonPasswords(string resourceName)
    {
        var assembly = Assembly.GetExecutingAssembly();
        using var stream = assembly.GetManifestResourceStream(resourceName);
        if (stream is null)
            return new HashSet<string>(StringComparer.OrdinalIgnoreCase);

        using var reader = new StreamReader(stream);
        var passwords = new HashSet<string>(StringComparer.OrdinalIgnoreCase);
        string? line;
        while ((line = reader.ReadLine()) != null)
        {
            if (!string.IsNullOrWhiteSpace(line))
                passwords.Add(line.Trim());
        }
        return passwords;
    }
}
