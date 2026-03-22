using Microsoft.AspNetCore.Identity.UI.Services;
using Microsoft.Extensions.Options;
using rawdog.Options;

namespace rawdog.Services;

public sealed class FileEmailSender(IWebHostEnvironment environment, ILogger<FileEmailSender> logger, IOptions<EmailStorageOptions> options)
    : IEmailSender
{
    private readonly EmailStorageOptions _options = options.Value;

    public async Task SendEmailAsync(string email, string subject, string htmlMessage)
    {
        var rootPath = Path.Combine(environment.ContentRootPath, _options.RootPath);
        Directory.CreateDirectory(rootPath);

        var fileName = $"{DateTime.UtcNow:yyyyMMddHHmmssfff}-{Guid.NewGuid():N}.html";
        var targetPath = Path.Combine(rootPath, fileName);

        var body = $$"""
        <html>
        <body>
        <p><strong>To:</strong> {{email}}</p>
        <p><strong>Subject:</strong> {{subject}}</p>
        <hr />
        {{htmlMessage}}
        </body>
        </html>
        """;

        await File.WriteAllTextAsync(targetPath, body);
        logger.LogInformation("Wrote outbound email for {Email} to {Path}", email, targetPath);
    }
}
