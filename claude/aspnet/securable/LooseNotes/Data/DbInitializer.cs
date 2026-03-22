using LooseNotes.Models;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

/// <summary>
/// Seeds the database with roles and an initial admin account on first run.
/// Admin credentials come from configuration / environment, never hardcoded (Confidentiality).
/// </summary>
public static class DbInitializer
{
    public const string AdminRoleName = "Admin";
    public const string UserRoleName = "User";

    /// <summary>
    /// Applies pending migrations and seeds roles and the bootstrap admin.
    /// Safe to call on every startup — all operations are idempotent.
    /// </summary>
    public static async Task InitializeAsync(
        IServiceProvider services,
        IConfiguration configuration,
        ILogger logger)
    {
        using var scope = services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
        var roleManager = scope.ServiceProvider.GetRequiredService<RoleManager<IdentityRole>>();
        var userManager = scope.ServiceProvider.GetRequiredService<UserManager<ApplicationUser>>();

        await ApplyMigrationsAsync(db, logger);
        await SeedRolesAsync(roleManager, logger);
        await SeedAdminUserAsync(userManager, configuration, logger);
    }

    private static async Task ApplyMigrationsAsync(ApplicationDbContext db, ILogger logger)
    {
        try
        {
            await db.Database.MigrateAsync();
            logger.LogInformation("Database migrations applied successfully");
        }
        catch (Exception ex)
        {
            logger.LogError(ex, "Database migration failed");
            throw;
        }
    }

    private static async Task SeedRolesAsync(RoleManager<IdentityRole> roleManager, ILogger logger)
    {
        foreach (var roleName in new[] { AdminRoleName, UserRoleName })
        {
            if (await roleManager.RoleExistsAsync(roleName))
                continue;

            var result = await roleManager.CreateAsync(new IdentityRole(roleName));
            if (result.Succeeded)
                logger.LogInformation("Role {RoleName} created", roleName);
            else
                logger.LogError("Failed to create role {RoleName}: {Errors}", roleName, string.Join(", ", result.Errors.Select(e => e.Description)));
        }
    }

    private static async Task SeedAdminUserAsync(
        UserManager<ApplicationUser> userManager,
        IConfiguration configuration,
        ILogger logger)
    {
        // Admin credentials from configuration only — never hardcoded (Confidentiality)
        var adminEmail = configuration["Seed:AdminEmail"];
        var adminPassword = configuration["Seed:AdminPassword"];

        if (string.IsNullOrWhiteSpace(adminEmail) || string.IsNullOrWhiteSpace(adminPassword))
        {
            logger.LogWarning("Seed:AdminEmail or Seed:AdminPassword not configured — skipping admin seed");
            return;
        }

        var existing = await userManager.FindByEmailAsync(adminEmail);
        if (existing is not null)
            return;

        var admin = new ApplicationUser
        {
            UserName = adminEmail,
            Email = adminEmail,
            DisplayName = "Administrator",
            EmailConfirmed = true,
            CreatedAt = DateTime.UtcNow
        };

        var createResult = await userManager.CreateAsync(admin, adminPassword);
        if (!createResult.Succeeded)
        {
            logger.LogError("Failed to create admin user: {Errors}", string.Join(", ", createResult.Errors.Select(e => e.Description)));
            return;
        }

        var roleResult = await userManager.AddToRolesAsync(admin, new[] { AdminRoleName, UserRoleName });
        if (roleResult.Succeeded)
            logger.LogInformation("Admin user seeded successfully");
        else
            logger.LogError("Failed to assign roles to admin: {Errors}", string.Join(", ", roleResult.Errors.Select(e => e.Description)));
    }
}
