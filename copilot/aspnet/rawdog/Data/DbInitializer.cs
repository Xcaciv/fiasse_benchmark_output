using Microsoft.AspNetCore.Identity;
using LooseNotes.Models;

namespace LooseNotes.Data;

public static class DbInitializer
{
    public static async Task SeedAsync(IServiceProvider serviceProvider)
    {
        var roleManager = serviceProvider.GetRequiredService<RoleManager<IdentityRole>>();
        var userManager = serviceProvider.GetRequiredService<UserManager<ApplicationUser>>();

        string[] roles = { "Admin", "User" };
        foreach (var role in roles)
        {
            if (!await roleManager.RoleExistsAsync(role))
                await roleManager.CreateAsync(new IdentityRole(role));
        }

        if (await userManager.FindByEmailAsync("admin@example.com") == null)
        {
            var admin = new ApplicationUser
            {
                UserName = "admin",
                Email = "admin@example.com",
                EmailConfirmed = true,
                CreatedAt = DateTime.UtcNow
            };
            var result = await userManager.CreateAsync(admin, "Admin@123");
            if (result.Succeeded)
                await userManager.AddToRoleAsync(admin, "Admin");
        }

        if (await userManager.FindByEmailAsync("user1@example.com") == null)
        {
            var user = new ApplicationUser
            {
                UserName = "user1",
                Email = "user1@example.com",
                EmailConfirmed = true,
                CreatedAt = DateTime.UtcNow
            };
            var result = await userManager.CreateAsync(user, "User@123");
            if (result.Succeeded)
                await userManager.AddToRoleAsync(user, "User");
        }
    }
}
