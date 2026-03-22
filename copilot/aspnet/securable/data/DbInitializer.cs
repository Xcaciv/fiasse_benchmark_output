using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using LooseNotes.Models;
using LooseNotes.Options;

namespace LooseNotes.Data;

public static class DbInitializer
{
    public static async Task SeedAsync(IServiceProvider services, IWebHostEnvironment environment)
    {
        var roleManager = services.GetRequiredService<RoleManager<IdentityRole>>();
        var userManager = services.GetRequiredService<UserManager<ApplicationUser>>();
        var logger = services.GetRequiredService<ILoggerFactory>().CreateLogger("DbInitializer");
        var bootstrapOptions = services.GetRequiredService<IOptions<BootstrapAdminOptions>>().Value;

        foreach (var roleName in new[] { "User", "Admin" })
        {
            if (!await roleManager.RoleExistsAsync(roleName))
            {
                var roleResult = await roleManager.CreateAsync(new IdentityRole(roleName));
                if (!roleResult.Succeeded)
                {
                    throw new InvalidOperationException($"Failed to create role {roleName}: {string.Join(", ", roleResult.Errors.Select(x => x.Description))}");
                }
            }
        }

        if (string.IsNullOrWhiteSpace(bootstrapOptions.Email) || string.IsNullOrWhiteSpace(bootstrapOptions.Password))
        {
            logger.LogInformation("Bootstrap admin account was not configured. Skipping admin creation.");
            return;
        }

        var existingUser = await userManager.Users.FirstOrDefaultAsync(x => x.Email == bootstrapOptions.Email);
        if (existingUser is null)
        {
            existingUser = new ApplicationUser
            {
                UserName = string.IsNullOrWhiteSpace(bootstrapOptions.UserName) ? bootstrapOptions.Email : bootstrapOptions.UserName,
                Email = bootstrapOptions.Email,
                RegisteredAtUtc = DateTime.UtcNow,
                EmailConfirmed = true
            };

            var createResult = await userManager.CreateAsync(existingUser, bootstrapOptions.Password);
            if (!createResult.Succeeded)
            {
                throw new InvalidOperationException($"Failed to create bootstrap admin: {string.Join(", ", createResult.Errors.Select(x => x.Description))}");
            }
        }

        foreach (var roleName in new[] { "User", "Admin" })
        {
            if (!await userManager.IsInRoleAsync(existingUser, roleName))
            {
                var addResult = await userManager.AddToRoleAsync(existingUser, roleName);
                if (!addResult.Succeeded)
                {
                    throw new InvalidOperationException($"Failed to assign role {roleName}: {string.Join(", ", addResult.Errors.Select(x => x.Description))}");
                }
            }
        }
    }
}
