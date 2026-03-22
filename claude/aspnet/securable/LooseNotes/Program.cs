using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using Serilog;
using Serilog.Events;

// Configure Serilog early so startup errors are captured.
// SSEM: Never log passwords, tokens, or PII in structured logs.
Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Information()
    .MinimumLevel.Override("Microsoft", LogEventLevel.Warning)
    .MinimumLevel.Override("Microsoft.Hosting.Lifetime", LogEventLevel.Information)
    .WriteTo.Console()
    .WriteTo.File("logs/loosenotes-.log", rollingInterval: RollingInterval.Day,
        retainedFileCountLimit: 30)
    .CreateBootstrapLogger();

try
{
    Log.Information("Starting LooseNotes application");

    var builder = WebApplication.CreateBuilder(args);

    // Use Serilog for all host logging
    builder.Host.UseSerilog((ctx, services, cfg) =>
        cfg.ReadFrom.Configuration(ctx.Configuration)
           .ReadFrom.Services(services)
           .Enrich.FromLogContext()
           .WriteTo.Console()
           .WriteTo.File("logs/loosenotes-.log", rollingInterval: RollingInterval.Day,
               retainedFileCountLimit: 30));

    // -----------------------------------------------------------------------
    // Database
    // -----------------------------------------------------------------------
    var connectionString = builder.Configuration.GetConnectionString("DefaultConnection")
        ?? "Data Source=loosenotes.db";

    builder.Services.AddDbContext<ApplicationDbContext>(options =>
        options.UseSqlite(connectionString));

    // -----------------------------------------------------------------------
    // Identity  (FIASSE: strong password policy, lockout, hashing via PBKDF2)
    // -----------------------------------------------------------------------
    builder.Services.AddIdentity<ApplicationUser, IdentityRole>(options =>
    {
        // Password policy
        options.Password.RequiredLength = 10;
        options.Password.RequireDigit = true;
        options.Password.RequireLowercase = true;
        options.Password.RequireUppercase = true;
        options.Password.RequireNonAlphanumeric = true;

        // Account lockout – fail-safe: lock after 5 bad attempts for 15 min
        options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(15);
        options.Lockout.MaxFailedAccessAttempts = 5;
        options.Lockout.AllowedForNewUsers = true;

        // Require unique email
        options.User.RequireUniqueEmail = true;

        // Sign-in
        options.SignIn.RequireConfirmedAccount = false; // simplified for demo
    })
    .AddEntityFrameworkStores<ApplicationDbContext>()
    .AddDefaultTokenProviders();

    // Cookie security (SSEM: HttpOnly, Secure, SameSite=Lax)
    builder.Services.ConfigureApplicationCookie(options =>
    {
        options.Cookie.HttpOnly = true;
        options.Cookie.SecurePolicy = CookieSecurePolicy.Always;
        options.Cookie.SameSite = SameSiteMode.Lax;
        options.ExpireTimeSpan = TimeSpan.FromHours(2);
        options.SlidingExpiration = true;
        options.LoginPath = "/Account/Login";
        options.LogoutPath = "/Account/Logout";
        options.AccessDeniedPath = "/Account/AccessDenied";
    });

    // -----------------------------------------------------------------------
    // Anti-forgery (CSRF protection on all state-changing requests)
    // -----------------------------------------------------------------------
    builder.Services.AddAntiforgery(options =>
    {
        options.Cookie.HttpOnly = true;
        options.Cookie.SecurePolicy = CookieSecurePolicy.Always;
        options.Cookie.SameSite = SameSiteMode.Strict;
        options.HeaderName = "X-CSRF-TOKEN";
    });

    // -----------------------------------------------------------------------
    // MVC + Razor
    // -----------------------------------------------------------------------
    builder.Services.AddControllersWithViews(options =>
    {
        // Global anti-forgery filter so every POST is validated
        options.Filters.Add(new Microsoft.AspNetCore.Mvc.AutoValidateAntiforgeryTokenAttribute());
    });

    // -----------------------------------------------------------------------
    // Application services
    // -----------------------------------------------------------------------
    builder.Services.AddScoped<IFileStorageService, LocalFileStorageService>();
    builder.Services.AddTransient<IEmailService, LoggingEmailService>();
    builder.Services.AddScoped<IAuditService, AuditService>();

    // -----------------------------------------------------------------------
    // HTTP security headers middleware (added via middleware below)
    // -----------------------------------------------------------------------

    var app = builder.Build();

    // -----------------------------------------------------------------------
    // Seed database roles and admin user
    // -----------------------------------------------------------------------
    using (var scope = app.Services.CreateScope())
    {
        var services = scope.ServiceProvider;
        try
        {
            await DbInitializer.SeedAsync(services);
        }
        catch (Exception ex)
        {
            Log.Error(ex, "An error occurred while seeding the database");
        }
    }

    // -----------------------------------------------------------------------
    // Middleware pipeline
    // -----------------------------------------------------------------------
    if (!app.Environment.IsDevelopment())
    {
        app.UseExceptionHandler("/Home/Error");
        app.UseHsts();
    }

    app.UseHttpsRedirection();

    // Security headers (SSEM: defence-in-depth)
    app.Use(async (ctx, next) =>
    {
        ctx.Response.Headers["X-Content-Type-Options"] = "nosniff";
        ctx.Response.Headers["X-Frame-Options"] = "DENY";
        ctx.Response.Headers["X-XSS-Protection"] = "1; mode=block";
        ctx.Response.Headers["Referrer-Policy"] = "strict-origin-when-cross-origin";
        ctx.Response.Headers["Permissions-Policy"] = "camera=(), microphone=(), geolocation=()";
        ctx.Response.Headers["Content-Security-Policy"] =
            "default-src 'self'; " +
            "script-src 'self' https://cdn.jsdelivr.net; " +
            "style-src 'self' https://cdn.jsdelivr.net; " +
            "img-src 'self' data:; " +
            "font-src 'self' https://cdn.jsdelivr.net; " +
            "frame-ancestors 'none';";
        await next();
    });

    app.UseStaticFiles();
    app.UseRouting();

    app.UseSerilogRequestLogging(options =>
    {
        // Suppress health-check noise, do NOT log query strings (may contain tokens)
        options.MessageTemplate = "HTTP {RequestMethod} {RequestPath} responded {StatusCode} in {Elapsed:0.0000} ms";
    });

    app.UseAuthentication();
    app.UseAuthorization();

    app.MapControllerRoute(
        name: "default",
        pattern: "{controller=Home}/{action=Index}/{id?}");

    await app.RunAsync();
}
catch (Exception ex)
{
    Log.Fatal(ex, "Application terminated unexpectedly");
}
finally
{
    Log.CloseAndFlush();
}
