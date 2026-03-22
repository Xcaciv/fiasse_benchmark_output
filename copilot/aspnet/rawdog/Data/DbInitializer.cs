using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using rawdog.Models;
using rawdog.Options;

namespace rawdog.Data;

public static class DbInitializer
{
    public static async Task InitializeAsync(IServiceProvider serviceProvider)
    {
        using var scope = serviceProvider.CreateScope();
        var scopedProvider = scope.ServiceProvider;

        var dbContext = scopedProvider.GetRequiredService<ApplicationDbContext>();
        var roleManager = scopedProvider.GetRequiredService<RoleManager<IdentityRole>>();
        var userManager = scopedProvider.GetRequiredService<UserManager<ApplicationUser>>();
        var adminOptions = scopedProvider.GetRequiredService<IOptions<SeedAdminOptions>>().Value;

        await dbContext.Database.EnsureCreatedAsync();

        foreach (var roleName in new[] { "User", "Admin" })
        {
            if (!await roleManager.RoleExistsAsync(roleName))
            {
                var createRoleResult = await roleManager.CreateAsync(new IdentityRole(roleName));
                if (!createRoleResult.Succeeded)
                {
                    throw new InvalidOperationException($"Failed to create role '{roleName}': {string.Join("; ", createRoleResult.Errors.Select(error => error.Description))}");
                }
            }
        }

        var adminUser = await userManager.Users.SingleOrDefaultAsync(user => user.Email == adminOptions.Email);
        if (adminUser is null)
        {
            adminUser = new ApplicationUser
            {
                UserName = adminOptions.UserName,
                Email = adminOptions.Email,
                EmailConfirmed = true,
                RegisteredAtUtc = DateTime.UtcNow
            };

            var createAdminResult = await userManager.CreateAsync(adminUser, adminOptions.Password);
            if (!createAdminResult.Succeeded)
            {
                throw new InvalidOperationException($"Failed to create the seed admin user: {string.Join("; ", createAdminResult.Errors.Select(error => error.Description))}");
            }
        }

        foreach (var roleName in new[] { "User", "Admin" })
        {
            if (!await userManager.IsInRoleAsync(adminUser, roleName))
            {
                var addToRoleResult = await userManager.AddToRoleAsync(adminUser, roleName);
                if (!addToRoleResult.Succeeded)
                {
                    throw new InvalidOperationException($"Failed to add the seed admin user to role '{roleName}': {string.Join("; ", addToRoleResult.Errors.Select(error => error.Description))}");
                }
            }
        }

        var usersWithoutRoles = await userManager.Users.ToListAsync();
        foreach (var user in usersWithoutRoles)
        {
            var roles = await userManager.GetRolesAsync(user);
            if (roles.Count == 0)
            {
                var assignResult = await userManager.AddToRoleAsync(user, "User");
                if (!assignResult.Succeeded)
                {
                    throw new InvalidOperationException($"Failed to add default role to user '{user.UserName}': {string.Join("; ", assignResult.Errors.Select(error => error.Description))}");
                }
            }
        }
    }
}
