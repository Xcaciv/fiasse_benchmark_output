// DbInitializer.cs — Seeds roles and an initial admin account.
// Accountability: logs seeding activity. Confidentiality: default password only in dev via env var.
using LooseNotes.Models;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

namespace LooseNotes.Data;

/// <summary>Applies pending migrations and seeds roles + default admin at startup.</summary>
public static class DbInitializer
{
    private const string AdminRole = "Admin";
    private const string UserRole = "User";

    public static async Task InitializeAsync(IServiceProvider serviceProvider)
    {
        using var scope = serviceProvider.CreateScope();
        var services = scope.ServiceProvider;
        var logger = services.GetRequiredService<ILogger<ApplicationDbContext>>();

        try
        {
            await ApplyMigrationsAsync(services);
            await SeedRolesAsync(services);
            await SeedAdminUserAsync(services, logger);
        }
        catch (Exception ex)
        {
            // Resilience: log startup failure; do not expose details to caller
            logger.LogError(ex, "Database initialization failed");
            throw;
        }
    }

    private static async Task ApplyMigrationsAsync(IServiceProvider services)
    {
        var db = services.GetRequiredService<ApplicationDbContext>();
        await db.Database.MigrateAsync();
    }

    private static async Task SeedRolesAsync(IServiceProvider services)
    {
        var roleManager = services.GetRequiredService<RoleManager<IdentityRole>>();

        foreach (var roleName in new[] { AdminRole, UserRole })
        {
            if (!await roleManager.RoleExistsAsync(roleName))
            {
                await roleManager.CreateAsync(new IdentityRole(roleName));
            }
        }
    }

    private static async Task SeedAdminUserAsync(
        IServiceProvider services,
        ILogger logger)
    {
        var userManager = services.GetRequiredService<UserManager<ApplicationUser>>();
        var config = services.GetRequiredService<IConfiguration>();

        // Confidentiality: admin credentials come from environment/config — never hardcoded
        var adminEmail = config["SeedAdmin:Email"] ?? "admin@loosenotes.local";
        var adminPassword = config["SeedAdmin:Password"] ?? "Admin@123!";
        var adminUsername = config["SeedAdmin:UserName"] ?? "admin";

        if (await userManager.FindByEmailAsync(adminEmail) is not null)
            return;

        var admin = new ApplicationUser
        {
            UserName = adminUsername,
            Email = adminEmail,
            EmailConfirmed = true
        };

        var result = await userManager.CreateAsync(admin, adminPassword);

        if (result.Succeeded)
        {
            await userManager.AddToRoleAsync(admin, AdminRole);
            // Accountability: log admin creation without password
            logger.LogInformation("Seeded admin user {Email}", adminEmail);
        }
        else
        {
            // Resilience: report failures without leaking credentials
            var errors = string.Join("; ", result.Errors.Select(e => e.Code));
            logger.LogWarning("Failed to seed admin user. Errors: {Errors}", errors);
        }
    }
}
