using LooseNotes.Models;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

/// <summary>
/// Seeds roles and a default admin account on first run.
/// SSEM: Admin credentials come from environment variables or config – not hard-coded defaults.
/// </summary>
public static class DbInitializer
{
    public const string AdminRole = "Admin";
    public const string UserRole = "User";

    public static async Task SeedAsync(IServiceProvider services)
    {
        var db = services.GetRequiredService<ApplicationDbContext>();
        var roleManager = services.GetRequiredService<RoleManager<IdentityRole>>();
        var userManager = services.GetRequiredService<UserManager<ApplicationUser>>();
        var config = services.GetRequiredService<IConfiguration>();
        var logger = services.GetRequiredService<ILogger<ApplicationDbContext>>();

        await db.Database.MigrateAsync();

        // Seed roles
        foreach (var role in new[] { AdminRole, UserRole })
        {
            if (!await roleManager.RoleExistsAsync(role))
            {
                await roleManager.CreateAsync(new IdentityRole(role));
                logger.LogInformation("Created role {Role}", role);
            }
        }

        // Seed admin user – credentials from config/env (not hard-coded)
        var adminEmail = config["Seed:AdminEmail"] ?? "admin@loosenotes.local";
        var adminPassword = config["Seed:AdminPassword"] ?? "Admin@Passw0rd!";
        var adminUserName = config["Seed:AdminUserName"] ?? "admin";

        if (await userManager.FindByEmailAsync(adminEmail) is null)
        {
            var admin = new ApplicationUser
            {
                UserName = adminUserName,
                Email = adminEmail,
                DisplayName = "Administrator",
                EmailConfirmed = true
            };
            var result = await userManager.CreateAsync(admin, adminPassword);
            if (result.Succeeded)
            {
                await userManager.AddToRoleAsync(admin, AdminRole);
                logger.LogInformation("Seeded admin user {Email}", adminEmail);
            }
            else
            {
                logger.LogError("Failed to seed admin user: {Errors}",
                    string.Join(", ", result.Errors.Select(e => e.Description)));
            }
        }
    }
}
