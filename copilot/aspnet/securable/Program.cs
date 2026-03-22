using System.Threading.RateLimiting;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using LooseNotes.Services.Interfaces;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// ── Database ──────────────────────────────────────────────────────────────────
builder.Services.AddDbContext<ApplicationDbContext>(options =>
    options.UseSqlite(builder.Configuration.GetConnectionString("DefaultConnection")));

// ── Identity (Authenticity) ───────────────────────────────────────────────────
builder.Services.AddIdentity<ApplicationUser, IdentityRole>(ConfigureIdentity)
    .AddEntityFrameworkStores<ApplicationDbContext>()
    .AddDefaultTokenProviders();

// ── Cookie Auth (Authenticity + Confidentiality) ──────────────────────────────
builder.Services.ConfigureApplicationCookie(options =>
{
    options.LoginPath = "/Account/Login";
    options.LogoutPath = "/Account/Logout";
    options.AccessDeniedPath = "/Account/Login";
    options.SlidingExpiration = true;
    options.ExpireTimeSpan = TimeSpan.FromMinutes(30);
    options.Cookie.HttpOnly = true;
    options.Cookie.SecurePolicy = CookieSecurePolicy.Always;
    options.Cookie.SameSite = SameSiteMode.Strict;
});

// ── Rate Limiting (Availability) ──────────────────────────────────────────────
builder.Services.AddRateLimiter(ConfigureRateLimiter);

// ── Anti-forgery (Integrity) ──────────────────────────────────────────────────
builder.Services.AddAntiforgery(options =>
{
    options.Cookie.SecurePolicy = CookieSecurePolicy.Always;
    options.Cookie.SameSite = SameSiteMode.Strict;
});

// ── Application Services (Modifiability via DI) ───────────────────────────────
builder.Services.AddScoped<INoteService, NoteService>();
builder.Services.AddScoped<IFileStorageService, FileStorageService>();
builder.Services.AddScoped<IShareLinkService, ShareLinkService>();
builder.Services.AddScoped<IAuditService, AuditService>();
builder.Services.AddScoped<IEmailService, EmailService>();

builder.Services.AddControllersWithViews();

var app = builder.Build();

// ── Middleware Pipeline ───────────────────────────────────────────────────────
if (!app.Environment.IsDevelopment())
{
    app.UseExceptionHandler("/Home/Error");
    app.UseHsts();
}

app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseRouting();
app.UseRateLimiter();
app.UseAuthentication();
app.UseAuthorization();

app.MapControllerRoute(name: "default", pattern: "{controller=Home}/{action=Index}/{id?}");

// ── Seed (Availability: app starts even if seed fails) ────────────────────────
await SeedDatabaseAsync(app);

app.Run();

// ── Configuration Helpers ────────────────────────────────────────────────────

static void ConfigureIdentity(IdentityOptions options)
{
    options.Password.RequireDigit = true;
    options.Password.RequireUppercase = true;
    options.Password.RequireNonAlphanumeric = true;
    options.Password.RequiredLength = 8;
    options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(15);
    options.Lockout.MaxFailedAccessAttempts = 5;
    options.Lockout.AllowedForNewUsers = true;
    options.User.RequireUniqueEmail = true;
}

static void ConfigureRateLimiter(RateLimiterOptions options)
{
    options.AddFixedWindowLimiter("default", limiterOptions =>
    {
        limiterOptions.PermitLimit = 100;
        limiterOptions.Window = TimeSpan.FromMinutes(1);
        limiterOptions.QueueProcessingOrder = QueueProcessingOrder.OldestFirst;
        limiterOptions.QueueLimit = 10;
    });

    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
    options.OnRejected = async (context, _) =>
    {
        context.HttpContext.Response.StatusCode = StatusCodes.Status429TooManyRequests;
        await context.HttpContext.Response.WriteAsync("Too many requests. Please try again later.");
    };
}

static async Task SeedDatabaseAsync(WebApplication app)
{
    using var scope = app.Services.CreateScope();
    var services = scope.ServiceProvider;

    try
    {
        var db = services.GetRequiredService<ApplicationDbContext>();
        await db.Database.MigrateAsync();

        await SeedRolesAsync(services);
        await SeedAdminUserAsync(services, app.Configuration, app.Logger);
    }
    catch (Exception ex)
    {
        app.Logger.LogError(ex, "Database seeding failed — application will continue");
    }
}

static async Task SeedRolesAsync(IServiceProvider services)
{
    var roleManager = services.GetRequiredService<RoleManager<IdentityRole>>();

    foreach (var role in new[] { "Admin", "User" })
    {
        if (!await roleManager.RoleExistsAsync(role))
        {
            await roleManager.CreateAsync(new IdentityRole(role));
        }
    }
}

static async Task SeedAdminUserAsync(IServiceProvider services, IConfiguration config, ILogger logger)
{
    var userManager = services.GetRequiredService<UserManager<ApplicationUser>>();

    var adminUsername = config["Identity:DefaultAdminUsername"] ?? "admin";
    var adminEmail = config["Identity:DefaultAdminEmail"] ?? "admin@localhost";

    if (await userManager.FindByNameAsync(adminUsername) is not null) return;

    // Admin password sourced from env var — never hardcoded (Confidentiality)
    var adminPassword = Environment.GetEnvironmentVariable("ADMIN_PASSWORD");
    if (string.IsNullOrWhiteSpace(adminPassword))
    {
        adminPassword = $"Admin@{Guid.NewGuid():N}"[..16] + "1!";
        // Log to console only (not structured log sinks) — token-safe output
        Console.WriteLine($"[STARTUP] Generated admin password: {adminPassword}");
    }

    var admin = new ApplicationUser { UserName = adminUsername, Email = adminEmail, EmailConfirmed = true };
    var result = await userManager.CreateAsync(admin, adminPassword);

    if (result.Succeeded)
    {
        await userManager.AddToRoleAsync(admin, "Admin");
        logger.LogInformation("Admin user '{Username}' seeded successfully", adminUsername);
    }
    else
    {
        logger.LogError("Failed to seed admin user: {Errors}",
            string.Join(", ", result.Errors.Select(e => e.Code)));
    }
}
