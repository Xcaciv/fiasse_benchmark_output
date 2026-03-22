using System.Threading.RateLimiting;
using LooseNotes.Configuration;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;
using Serilog;

// ── Bootstrap Serilog early so startup errors are captured ────────────────────
Log.Logger = new LoggerConfiguration()
    .WriteTo.Console()
    .CreateBootstrapLogger();

try
{
    var builder = WebApplication.CreateBuilder(args);

    // ── Structured logging (Accountability, Transparency) ─────────────────────
    builder.Host.UseSerilog((ctx, services, cfg) =>
        cfg.ReadFrom.Configuration(ctx.Configuration)
           .ReadFrom.Services(services));

    ConfigureServices(builder);

    var app = builder.Build();

    ConfigureMiddleware(app);

    // ── Database initialization (idempotent) ──────────────────────────────────
    await DbInitializer.InitializeAsync(app.Services, app.Configuration,
        app.Services.GetRequiredService<ILogger<Program>>());

    app.Run();
}
catch (Exception ex)
{
    Log.Fatal(ex, "Application startup failed");
    throw;
}
finally
{
    Log.CloseAndFlush();
}

// ── Service registration ───────────────────────────────────────────────────────

static void ConfigureServices(WebApplicationBuilder builder)
{
    var services = builder.Services;
    var config = builder.Configuration;

    // Strongly-typed config (Modifiability — no magic strings in services)
    services.Configure<FileStorageOptions>(config.GetSection(FileStorageOptions.SectionName));
    services.Configure<SecurityOptions>(config.GetSection(SecurityOptions.SectionName));
    services.Configure<RateLimitingOptions>(config.GetSection(RateLimitingOptions.SectionName));

    // EF Core (all queries parameterized by ORM — Integrity)
    services.AddDbContext<ApplicationDbContext>(opts =>
        opts.UseSqlite(config.GetConnectionString("DefaultConnection")));

    // ASP.NET Core Identity (Authenticity)
    services.AddIdentity<ApplicationUser, IdentityRole>(opts =>
    {
        // Password policy — enforce minimum complexity (Authenticity)
        opts.Password.RequireDigit = true;
        opts.Password.RequireLowercase = true;
        opts.Password.RequireUppercase = true;
        opts.Password.RequireNonAlphanumeric = false;
        opts.Password.RequiredLength = 8;

        // Account lockout after N failures (Availability, defense-in-depth)
        opts.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(5);
        opts.Lockout.MaxFailedAccessAttempts = 5;
        opts.Lockout.AllowedForNewUsers = true;

        // Require unique email (Integrity)
        opts.User.RequireUniqueEmail = true;
    })
    .AddEntityFrameworkStores<ApplicationDbContext>()
    .AddDefaultTokenProviders();

    // Cookie settings (Authenticity, Confidentiality)
    services.ConfigureApplicationCookie(opts =>
    {
        opts.LoginPath = "/Account/Login";
        opts.LogoutPath = "/Account/Logout";
        opts.AccessDeniedPath = "/Account/AccessDenied";
        opts.ExpireTimeSpan = TimeSpan.FromHours(8);
        opts.SlidingExpiration = true;
        // HttpOnly and Secure prevent JS access and force HTTPS in production
        opts.Cookie.HttpOnly = true;
        opts.Cookie.SecurePolicy = CookieSecurePolicy.SameAsRequest;
        opts.Cookie.SameSite = SameSiteMode.Lax;
        opts.Cookie.Name = "LN_Auth";
    });

    // Application services (Modifiability — all injected via interface)
    services.AddScoped<IAuditService, AuditService>();
    services.AddScoped<IFileStorageService, LocalFileStorageService>();
    services.AddScoped<IEmailService, LoggingEmailService>();
    services.AddScoped<IShareTokenService, ShareTokenService>();

    // Rate limiting (Availability)
    ConfigureRateLimiting(services, builder.Configuration);

    // Antiforgery (Integrity — CSRF protection on all state-changing forms)
    services.AddAntiforgery(opts =>
    {
        opts.Cookie.HttpOnly = true;
        opts.Cookie.SecurePolicy = CookieSecurePolicy.SameAsRequest;
        opts.Cookie.Name = "LN_XSRF";
    });

    // MVC
    services.AddControllersWithViews();
}

// ── Rate limiter configuration ─────────────────────────────────────────────────

static void ConfigureRateLimiting(IServiceCollection services, IConfiguration config)
{
    var rlOpts = config.GetSection(RateLimitingOptions.SectionName).Get<RateLimitingOptions>()
                 ?? new RateLimitingOptions();

    services.AddRateLimiter(opts =>
    {
        opts.RejectionStatusCode = StatusCodes.Status429TooManyRequests;

        // Per-IP fixed-window limiter for login (Availability)
        opts.AddFixedWindowLimiter("login", limiter =>
        {
            limiter.Window = TimeSpan.FromSeconds(rlOpts.LoginWindowSeconds);
            limiter.PermitLimit = rlOpts.LoginMaxAttempts;
            limiter.QueueProcessingOrder = QueueProcessingOrder.OldestFirst;
            limiter.QueueLimit = 0;
        });

        // Per-IP fixed-window limiter for registration
        opts.AddFixedWindowLimiter("register", limiter =>
        {
            limiter.Window = TimeSpan.FromSeconds(rlOpts.RegisterWindowSeconds);
            limiter.PermitLimit = rlOpts.RegisterMaxAttempts;
            limiter.QueueProcessingOrder = QueueProcessingOrder.OldestFirst;
            limiter.QueueLimit = 0;
        });
    });
}

// ── Middleware pipeline ────────────────────────────────────────────────────────

static void ConfigureMiddleware(WebApplication app)
{
    if (!app.Environment.IsDevelopment())
    {
        // Generic error page — no stack traces to end users (Confidentiality)
        app.UseExceptionHandler("/Home/Error");
        // HSTS: instruct browsers to use HTTPS only (Confidentiality in transit)
        app.UseHsts();
    }

    // Add security headers to all responses
    app.Use(async (ctx, next) =>
    {
        ctx.Response.Headers.Append("X-Content-Type-Options", "nosniff");
        ctx.Response.Headers.Append("X-Frame-Options", "DENY");
        ctx.Response.Headers.Append("Referrer-Policy", "strict-origin-when-cross-origin");
        ctx.Response.Headers.Append("Permissions-Policy", "camera=(), microphone=(), geolocation=()");
        await next();
    });

    app.UseHttpsRedirection();
    app.UseStaticFiles();

    app.UseRouting();
    app.UseRateLimiter();

    app.UseAuthentication();
    app.UseAuthorization();

    // Serilog request logging (Accountability, Transparency)
    app.UseSerilogRequestLogging(opts =>
    {
        opts.EnrichDiagnosticContext = (diagCtx, httpCtx) =>
        {
            diagCtx.Set("UserId", httpCtx.User.FindFirst(System.Security.Claims.ClaimTypes.NameIdentifier)?.Value ?? "anon");
            diagCtx.Set("IP", httpCtx.Connection.RemoteIpAddress?.ToString() ?? "unknown");
        };
    });

    // Conventional route
    app.MapControllerRoute(
        name: "default",
        pattern: "{controller=Home}/{action=Index}/{id?}");

    // Share link route (Share/View uses token in segment — no path traversal risk)
    app.MapControllerRoute(
        name: "share",
        pattern: "share/{token}",
        defaults: new { controller = "Share", action = "View" });
}
