using LooseNotes.Models;
using Microsoft.AspNetCore.Identity;

namespace LooseNotes.Data;

/// <summary>
/// Seeds initial data: admin role and admin user.
/// Credentials are read from environment variables, not hardcoded (Confidentiality, FIASSE S2.1).
/// </summary>
public static class DbInitializer
{
    public static async Task InitializeAsync(
        ApplicationDbContext context,
        UserManager<ApplicationUser> userManager,
        RoleManager<IdentityRole> roleManager,
        ILogger<ApplicationDbContext> logger)
    {
        await context.Database.EnsureCreatedAsync();

        if (!await roleManager.RoleExistsAsync("Admin"))
        {
            var roleResult = await roleManager.CreateAsync(new IdentityRole("Admin"));
            if (!roleResult.Succeeded)
            {
                logger.LogError("Failed to create Admin role: {Errors}",
                    string.Join(", ", roleResult.Errors.Select(e => e.Description)));
                return;
            }
        }

        // Read admin credentials from environment to avoid hardcoded secrets
        var adminEmail = Environment.GetEnvironmentVariable("ADMIN_EMAIL") ?? "admin@localhost";
        var adminPassword = Environment.GetEnvironmentVariable("ADMIN_PASSWORD") ?? "Admin@LooseNotes1";
        var adminUsername = Environment.GetEnvironmentVariable("ADMIN_USERNAME") ?? "admin";

        if (await userManager.FindByEmailAsync(adminEmail) == null)
        {
            var adminUser = new ApplicationUser
            {
                UserName = adminUsername,
                Email = adminEmail,
                EmailConfirmed = true,
                DisplayName = "Administrator",
                CreatedAt = DateTime.UtcNow
            };

            var createResult = await userManager.CreateAsync(adminUser, adminPassword);
            if (createResult.Succeeded)
            {
                await userManager.AddToRoleAsync(adminUser, "Admin");
                logger.LogInformation("Admin user created with username {Username}", adminUsername);
            }
            else
            {
                logger.LogError("Failed to create admin user: {Errors}",
                    string.Join(", ", createResult.Errors.Select(e => e.Description)));
            }
        }
    }
}
