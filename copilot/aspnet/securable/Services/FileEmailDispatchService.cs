using Microsoft.Extensions.Options;
using LooseNotes.Options;

namespace LooseNotes.Services;

public sealed class FileEmailDispatchService : IEmailDispatchService
{
    private readonly EmailOptions _options;
    private readonly IWebHostEnvironment _environment;

    public FileEmailDispatchService(IOptions<EmailOptions> options, IWebHostEnvironment environment)
    {
        _options = options.Value;
        _environment = environment;
    }

    public async Task SendPasswordResetAsync(string toEmail, string resetUrl, CancellationToken cancellationToken = default)
    {
        var root = Path.GetFullPath(Path.Combine(_environment.ContentRootPath, _options.OutboxPath));
        Directory.CreateDirectory(root);

        var fileName = $"password-reset-{DateTime.UtcNow:yyyyMMddHHmmss}-{Guid.NewGuid():N}.html";
        var fullPath = Path.Combine(root, fileName);
        var body = $"""
<!DOCTYPE html>
<html lang="en">
<head><meta charset="utf-8"><title>Password Reset</title></head>
<body>
  <p>Password reset requested for <strong>{System.Net.WebUtility.HtmlEncode(toEmail)}</strong>.</p>
  <p>The reset link is valid for 1 hour.</p>
  <p><a href="{System.Net.WebUtility.HtmlEncode(resetUrl)}">Reset your password</a></p>
  <p>If you did not request this, you can ignore this message.</p>
</body>
</html>
""";

        await File.WriteAllTextAsync(fullPath, body, cancellationToken);
    }
}
