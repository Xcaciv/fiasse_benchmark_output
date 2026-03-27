using LooseNotes.Models;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

/// <summary>
/// Seeds required roles and the default admin user on startup.
/// Admin password MUST be supplied via configuration or environment variable —
/// never hardcoded (Confidentiality principle).
/// </summary>
public static class DbInitializer
{
    public static async Task InitializeAsync(IServiceProvider serviceProvider)
    {
        var logger = serviceProvider.GetRequiredService<ILogger<ApplicationDbContext>>();
        try
        {
            var context = serviceProvider.GetRequiredService<ApplicationDbContext>();
            await context.Database.MigrateAsync();
            await SeedRolesAsync(serviceProvider, logger);
            await SeedAdminUserAsync(serviceProvider, logger);
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Database initialization failed");
            throw;
        }
    }

    private static async Task SeedRolesAsync(IServiceProvider services, ILogger logger)
    {
        var roleManager = services.GetRequiredService<RoleManager<IdentityRole>>();
        foreach (var role in new[] { "Admin", "User" })
        {
            if (await roleManager.RoleExistsAsync(role)) continue;
            var result = await roleManager.CreateAsync(new IdentityRole(role));
            if (result.Succeeded)
                logger.LogInformation("Created role {Role}", role);
            else
                logger.LogError("Failed to create role {Role}: {Errors}", role,
                    string.Join(", ", result.Errors.Select(e => e.Description)));
        }
    }

    private static async Task SeedAdminUserAsync(IServiceProvider services, ILogger logger)
    {
        var userManager = services.GetRequiredService<UserManager<ApplicationUser>>();
        var config = services.GetRequiredService<IConfiguration>();

        var adminEmail = config["AdminSetup:DefaultAdminEmail"] ?? "admin@example.com";
        var adminPassword = config["AdminSetup:DefaultAdminPassword"];

        if (string.IsNullOrWhiteSpace(adminPassword))
        {
            // Non-fatal: admin can be created manually
            logger.LogWarning(
                "Admin password not configured. Set AdminSetup__DefaultAdminPassword to seed admin.");
            return;
        }

        if (await userManager.FindByEmailAsync(adminEmail) is not null) return;

        var admin = new ApplicationUser
        {
            UserName = adminEmail,
            Email = adminEmail,
            DisplayName = "Administrator",
            EmailConfirmed = true,
            CreatedAt = DateTimeOffset.UtcNow
        };

        var result = await userManager.CreateAsync(admin, adminPassword);
        if (result.Succeeded)
        {
            await userManager.AddToRoleAsync(admin, "Admin");
            logger.LogInformation("Admin user seeded for ID {UserId}", admin.Id);
        }
        else
        {
            logger.LogError("Admin user seeding failed: {Errors}",
                string.Join(", ", result.Errors.Select(e => e.Description)));
        }
    }
}
