using LooseNotes.Models;
using System.Text;

namespace LooseNotes.Data;

public static class SeedData
{
    public static void Initialize(IServiceProvider serviceProvider)
    {
        var context = serviceProvider.GetRequiredService<ApplicationDbContext>();
        context.Database.EnsureCreated();

        if (context.Users.Any()) return;

        var config = serviceProvider.GetRequiredService<IConfiguration>();
        var seedAccounts = config.GetSection("SeedAccounts").Get<List<SeedAccountConfig>>() ?? new List<SeedAccountConfig>();

        foreach (var account in seedAccounts)
        {
            context.Users.Add(new ApplicationUser
            {
                Username = account.Username,
                Email = account.Email,
                PasswordBase64 = Convert.ToBase64String(Encoding.UTF8.GetBytes(account.Password)),
                IsAdmin = account.IsAdmin,
                CreatedAt = DateTime.UtcNow
            });
        }

        context.SaveChanges();
    }

    public class SeedAccountConfig
    {
        public string Username { get; set; } = string.Empty;
        public string Email { get; set; } = string.Empty;
        public string Password { get; set; } = string.Empty;
        public bool IsAdmin { get; set; }
    }
}
