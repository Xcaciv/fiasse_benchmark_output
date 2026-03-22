using Microsoft.AspNetCore.Identity;
using LooseNotes.Models;

namespace LooseNotes.Data;

public static class SeedData
{
    public static async Task InitializeAsync(IServiceProvider serviceProvider)
    {
        var roleManager = serviceProvider.GetRequiredService<RoleManager<IdentityRole>>();
        var userManager = serviceProvider.GetRequiredService<UserManager<ApplicationUser>>();

        // Ensure roles exist
        string[] roles = ["Admin", "User"];
        foreach (var role in roles)
        {
            if (!await roleManager.RoleExistsAsync(role))
            {
                await roleManager.CreateAsync(new IdentityRole(role));
            }
        }

        // Create default admin if none exists
        var admins = await userManager.GetUsersInRoleAsync("Admin");
        if (admins.Count == 0)
        {
            var adminUser = new ApplicationUser
            {
                UserName = "admin",
                Email = "admin@loosenotes.local",
                EmailConfirmed = true
            };
            var result = await userManager.CreateAsync(adminUser, "Admin@123456");
            if (result.Succeeded)
            {
                await userManager.AddToRoleAsync(adminUser, "Admin");
            }
        }
    }
}
