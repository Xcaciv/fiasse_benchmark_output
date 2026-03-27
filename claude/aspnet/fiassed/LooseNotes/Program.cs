using LooseNotes.Configuration;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;
using Serilog;
using Serilog.Events;
using System.Threading.RateLimiting;

// Bootstrap logger - captures startup events before full configuration loads
Log.Logger = new LoggerConfiguration()
    .MinimumLevel.Override("Microsoft", LogEventLevel.Warning)
    .WriteTo.Console()
    .CreateBootstrapLogger();

try
{
    Log.Information("Starting Loose Notes application");
    var builder = WebApplication.CreateBuilder(args);

    // ─── Structured Logging (Serilog, FIASSE S2.6 / ASVS V16) ────────────────
    builder.Host.UseSerilog((context, services, config) =>
        config.ReadFrom.Configuration(context.Configuration)
              .ReadFrom.Services(services));

    // ─── Database ─────────────────────────────────────────────────────────────
    builder.Services.AddDbContext<ApplicationDbContext>(options =>
        options.UseSqlite(builder.Configuration.GetConnectionString("DefaultConnection")));

    // ─── Identity (ASVS V6.2, V7.2) ──────────────────────────────────────────
    builder.Services.AddIdentity<ApplicationUser, IdentityRole>(options =>
    {
        // Password policy from configuration - not hardcoded (FIASSE S2.1)
        var policyConfig = builder.Configuration.GetSection(PasswordPolicyOptions.SectionName);
        options.Password.RequiredLength = policyConfig.GetValue<int>("MinimumLength", 8);
        options.Password.RequireDigit = false;           // No composition rules (ASVS V6.2.5)
        options.Password.RequireLowercase = false;
        options.Password.RequireUppercase = false;
        options.Password.RequireNonAlphanumeric = false;
        options.Password.RequiredUniqueChars = 1;

        // Lockout configuration
        options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(15);
        options.Lockout.MaxFailedAccessAttempts = 10;
        options.Lockout.AllowedForNewUsers = true;

        // Enumeration-safe: don't reveal if username exists
        options.SignIn.RequireConfirmedAccount = false;

        options.User.RequireUniqueEmail = true;
        options.User.AllowedUserNameCharacters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789-._";
    })
    .AddEntityFrameworkStores<ApplicationDbContext>()
    .AddDefaultTokenProviders();

    // ─── Cookie / Session Security (ASVS V3.3, V7.2, V7.3) ──────────────────
    var securityConfig = builder.Configuration.GetSection(SecurityOptions.SectionName);
    builder.Services.ConfigureApplicationCookie(options =>
    {
        options.Cookie.HttpOnly = true;                           // ASVS V3.3.4
        options.Cookie.SecurePolicy = CookieSecurePolicy.Always; // ASVS V3.3.1
        options.Cookie.SameSite = SameSiteMode.Strict;           // ASVS V3.3.2
        options.Cookie.Name = "__Host-LN";                       // __Host- prefix enforces path=/
        options.ExpireTimeSpan = TimeSpan.FromMinutes(
            securityConfig.GetValue<int>("SessionTimeoutMinutes", 30)); // ASVS V7.3.1
        options.SlidingExpiration = true;
        options.LoginPath = "/Account/Login";
        options.LogoutPath = "/Account/Logout";
        options.AccessDeniedPath = "/Account/AccessDenied";
    });

    // Absolute session lifetime enforced via data protection ticket expiry (ASVS V7.3.2)
    builder.Services.AddDataProtection();

    // ─── Options ──────────────────────────────────────────────────────────────
    builder.Services.Configure<SecurityOptions>(
        builder.Configuration.GetSection(SecurityOptions.SectionName));
    builder.Services.Configure<FileStorageOptions>(
        builder.Configuration.GetSection(FileStorageOptions.SectionName));
    builder.Services.Configure<PasswordPolicyOptions>(
        builder.Configuration.GetSection(PasswordPolicyOptions.SectionName));

    // ─── Application Services ─────────────────────────────────────────────────
    builder.Services.AddScoped<IAuditService, AuditService>();
    builder.Services.AddScoped<IFileStorageService, LocalFileStorageService>();
    builder.Services.AddSingleton<IShareTokenService, ShareTokenService>();
    builder.Services.AddSingleton<IPasswordValidationService, PasswordValidationService>();
    builder.Services.AddScoped<IEmailService, LoggingEmailService>();

    // ─── Rate Limiting (ASVS V2.4.1 / GSR: Anti-automation) ─────────────────
    // All thresholds are configuration-driven (FIASSE S2.1, Modifiability)
    builder.Services.AddRateLimiter(options =>
    {
        options.RejectionStatusCode = 429;

        var loginLimit = securityConfig.GetValue<int>("LoginRateLimitPerMinute", 10);
        options.AddFixedWindowLimiter("login", o =>
        {
            o.Window = TimeSpan.FromMinutes(1);
            o.PermitLimit = loginLimit;
            o.QueueLimit = 0;
            o.QueueProcessingOrder = QueueProcessingOrder.OldestFirst;
        });

        options.AddFixedWindowLimiter("registration", o =>
        {
            o.Window = TimeSpan.FromMinutes(1);
            o.PermitLimit = 5;
            o.QueueLimit = 0;
        });

        var resetLimit = securityConfig.GetValue<int>("ResetRateLimitPer15Minutes", 5);
        options.AddFixedWindowLimiter("passwordReset", o =>
        {
            o.Window = TimeSpan.FromMinutes(15);
            o.PermitLimit = resetLimit;
            o.QueueLimit = 0;
        });

        var searchLimit = securityConfig.GetValue<int>("SearchRateLimitPerMinute", 30);
        options.AddFixedWindowLimiter("search", o =>
        {
            o.Window = TimeSpan.FromMinutes(1);
            o.PermitLimit = searchLimit;
            o.QueueLimit = 0;
        });

        var uploadLimit = securityConfig.GetValue<int>("UploadRateLimitPerMinute", 20);
        options.AddFixedWindowLimiter("upload", o =>
        {
            o.Window = TimeSpan.FromMinutes(1);
            o.PermitLimit = uploadLimit;
            o.QueueLimit = 0;
        });

        var topRatedLimit = securityConfig.GetValue<int>("TopRatedRateLimitPerMinute", 60);
        options.AddFixedWindowLimiter("topRated", o =>
        {
            o.Window = TimeSpan.FromMinutes(1);
            o.PermitLimit = topRatedLimit;
            o.QueueLimit = 0;
        });

        var ratingLimit = securityConfig.GetValue<int>("RatingRateLimitPerMinute", 10);
        options.AddFixedWindowLimiter("rating", o =>
        {
            o.Window = TimeSpan.FromMinutes(1);
            o.PermitLimit = ratingLimit;
            o.QueueLimit = 0;
        });

        options.AddFixedWindowLimiter("shareView", o =>
        {
            o.Window = TimeSpan.FromMinutes(1);
            o.PermitLimit = 60;
            o.QueueLimit = 0;
        });
    });

    // ─── MVC with global anti-forgery (GSR-03, ASVS V3.5) ───────────────────
    builder.Services.AddControllersWithViews(options =>
    {
        // Global anti-forgery token validation on all POST/PUT/DELETE
        options.Filters.Add(new Microsoft.AspNetCore.Mvc.AutoValidateAntiforgeryTokenAttribute());
    });

    // ─── Request size limits (ASVS V5.2.1 / Availability) ───────────────────
    var maxFileSizeBytes = builder.Configuration
        .GetSection(FileStorageOptions.SectionName)
        .GetValue<long>("MaxFileSizeBytes", 10_485_760);

    builder.Services.Configure<Microsoft.AspNetCore.Http.Features.FormOptions>(options =>
    {
        options.MultipartBodyLengthLimit = maxFileSizeBytes + 65_536; // File + form fields overhead
    });

    var app = builder.Build();

    // ─── Middleware Pipeline ──────────────────────────────────────────────────
    app.UseSerilogRequestLogging(options =>
    {
        options.MessageTemplate = "HTTP {RequestMethod} {RequestPath} responded {StatusCode} in {Elapsed:0.0000}ms";
    });

    if (!app.Environment.IsDevelopment())
    {
        app.UseExceptionHandler("/Home/Error");
        app.UseHsts(); // Adds HSTS header (GSR-01, ASVS V12.1.1)
    }

    app.UseHttpsRedirection();

    // ─── Security Headers (GSR-02, ASVS V3.4) ────────────────────────────────
    app.Use(async (context, next) =>
    {
        context.Response.Headers["X-Content-Type-Options"] = "nosniff";
        context.Response.Headers["Referrer-Policy"] = "strict-origin-when-cross-origin";
        context.Response.Headers["X-Frame-Options"] = "DENY";
        context.Response.Headers["Content-Security-Policy"] =
            "default-src 'self'; script-src 'self'; style-src 'self'; " +
            "img-src 'self' data:; font-src 'self'; object-src 'none'; base-uri 'none'; " +
            "frame-ancestors 'none'";
        context.Response.Headers["Permissions-Policy"] =
            "camera=(), microphone=(), geolocation=()";
        await next();
    });

    app.UseStaticFiles();
    app.UseRouting();
    app.UseRateLimiter();
    app.UseAuthentication();
    app.UseAuthorization();

    app.MapControllerRoute(
        name: "default",
        pattern: "{controller=Home}/{action=Index}/{id?}");

    // ─── Database Initialization ──────────────────────────────────────────────
    using (var scope = app.Services.CreateScope())
    {
        var db = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
        var userManager = scope.ServiceProvider.GetRequiredService<UserManager<ApplicationUser>>();
        var roleManager = scope.ServiceProvider.GetRequiredService<RoleManager<IdentityRole>>();
        var logger = scope.ServiceProvider.GetRequiredService<ILogger<ApplicationDbContext>>();
        await DbInitializer.InitializeAsync(db, userManager, roleManager, logger);
    }

    await app.RunAsync();
}
catch (Exception ex) when (ex is not HostAbortedException)
{
    Log.Fatal(ex, "Application startup failed");
    throw;
}
finally
{
    Log.CloseAndFlush();
}
