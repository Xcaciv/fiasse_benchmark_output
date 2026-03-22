using LooseNotes.Models;
using Microsoft.AspNetCore.Identity;

namespace LooseNotes.Data;

public static class SeedData
{
    public const string AdminRole = "Admin";
    public const string UserRole = "User";

    public static async Task InitializeAsync(IServiceProvider serviceProvider)
    {
        var roleManager = serviceProvider.GetRequiredService<RoleManager<IdentityRole>>();
        var userManager = serviceProvider.GetRequiredService<UserManager<ApplicationUser>>();

        // Create roles
        foreach (var role in new[] { AdminRole, UserRole })
        {
            if (!await roleManager.RoleExistsAsync(role))
                await roleManager.CreateAsync(new IdentityRole(role));
        }

        // Create default admin user if none exists
        const string adminEmail = "admin@loosenotes.local";
        if (await userManager.FindByEmailAsync(adminEmail) == null)
        {
            var admin = new ApplicationUser
            {
                UserName = "admin",
                Email = adminEmail,
                EmailConfirmed = true
            };
            var result = await userManager.CreateAsync(admin, "Admin1234!");
            if (result.Succeeded)
            {
                await userManager.AddToRolesAsync(admin, new[] { AdminRole, UserRole });
            }
        }
    }
}
