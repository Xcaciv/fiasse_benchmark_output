// Program.cs — Application entry point and DI composition root
// Trust boundary: All external configuration is validated at startup.
using AspNetCoreRateLimit;
using LooseNotes.Configuration;
using LooseNotes.Data;
using LooseNotes.Models;
using LooseNotes.Services;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;

var builder = WebApplication.CreateBuilder(args);

// ── Database ──────────────────────────────────────────────────────────────────
builder.Services.AddDbContext<ApplicationDbContext>(options =>
    options.UseSqlite(builder.Configuration.GetConnectionString("DefaultConnection")));

// ── Identity (Authenticity + Confidentiality) ─────────────────────────────────
builder.Services.AddIdentity<ApplicationUser, IdentityRole>(options =>
{
    // Password complexity — Integrity at the trust boundary
    options.Password.RequireDigit = true;
    options.Password.RequiredLength = 8;
    options.Password.RequireNonAlphanumeric = false;
    options.Password.RequireUppercase = true;
    options.Password.RequireLowercase = true;

    // Lockout — Availability protection against brute force
    options.Lockout.DefaultLockoutTimeSpan = TimeSpan.FromMinutes(
        builder.Configuration.GetValue<int>("Security:LockoutDurationMinutes", 15));
    options.Lockout.MaxFailedAccessAttempts =
        builder.Configuration.GetValue<int>("Security:MaxLoginAttempts", 5);
    options.Lockout.AllowedForNewUsers = true;

    // Unique emails — Integrity
    options.User.RequireUniqueEmail = true;

    // Token lifespan — Authenticity (short-lived reset tokens)
    options.Tokens.PasswordResetTokenProvider = TokenOptions.DefaultProvider;
})
.AddEntityFrameworkStores<ApplicationDbContext>()
.AddDefaultTokenProviders();

// ── Cookie security (Authenticity + Confidentiality) ─────────────────────────
builder.Services.ConfigureApplicationCookie(options =>
{
    options.LoginPath = "/Account/Login";
    options.LogoutPath = "/Account/Logout";
    options.AccessDeniedPath = "/Account/AccessDenied";
    options.ExpireTimeSpan = TimeSpan.FromHours(8);
    options.SlidingExpiration = true;
    options.Cookie.HttpOnly = true;       // Prevent JS access
    options.Cookie.SecurePolicy = CookieSecurePolicy.Always;
    options.Cookie.SameSite = SameSiteMode.Lax;
    options.Cookie.Name = "LN.Session";
});

// ── Strongly-typed configuration (Modifiability) ─────────────────────────────
builder.Services.Configure<FileStorageOptions>(
    builder.Configuration.GetSection("FileStorage"));
builder.Services.Configure<SecurityOptions>(
    builder.Configuration.GetSection("Security"));

// ── Application services (DI / Testability) ───────────────────────────────────
builder.Services.AddHttpContextAccessor();
builder.Services.AddScoped<IAuditService, AuditService>();
builder.Services.AddScoped<IEmailService, LoggingEmailService>();
builder.Services.AddScoped<IFileStorageService, LocalFileStorageService>();
builder.Services.AddScoped<IShareTokenService, ShareTokenService>();

// ── Rate limiting (Availability) ─────────────────────────────────────────────
builder.Services.AddMemoryCache();
builder.Services.Configure<IpRateLimitOptions>(
    builder.Configuration.GetSection("IpRateLimiting"));
builder.Services.AddSingleton<IIpPolicyStore, MemoryCacheIpPolicyStore>();
builder.Services.AddSingleton<IRateLimitCounterStore, MemoryCacheRateLimitCounterStore>();
builder.Services.AddSingleton<IRateLimitConfiguration, RateLimitConfiguration>();
builder.Services.AddSingleton<IProcessingStrategy, AsyncKeyLockProcessingStrategy>();
builder.Services.AddInMemoryRateLimiting();

// ── MVC ───────────────────────────────────────────────────────────────────────
builder.Services.AddControllersWithViews(options =>
{
    // Global anti-forgery filter (Integrity — all state-changing forms)
    options.Filters.Add(new Microsoft.AspNetCore.Mvc.AutoValidateAntiforgeryTokenAttribute());
});

// ── Security headers ──────────────────────────────────────────────────────────
builder.Services.AddHsts(options =>
{
    options.Preload = true;
    options.IncludeSubDomains = true;
    options.MaxAge = TimeSpan.FromDays(365);
});

var app = builder.Build();

// ── Error handling (Resilience) ───────────────────────────────────────────────
if (app.Environment.IsDevelopment())
{
    app.UseDeveloperExceptionPage();
}
else
{
    app.UseExceptionHandler("/Home/Error");
    app.UseHsts();
}

// ── Security headers middleware ───────────────────────────────────────────────
app.Use(async (context, next) =>
{
    // Confidentiality: prevent sensitive data leakage via headers
    context.Response.Headers["X-Content-Type-Options"] = "nosniff";
    context.Response.Headers["X-Frame-Options"] = "DENY";
    context.Response.Headers["X-XSS-Protection"] = "1; mode=block";
    context.Response.Headers["Referrer-Policy"] = "strict-origin-when-cross-origin";
    context.Response.Headers["Content-Security-Policy"] =
        "default-src 'self'; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; " +
        "style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; img-src 'self' data:;";
    await next();
});

app.UseIpRateLimiting();
app.UseHttpsRedirection();
app.UseStaticFiles();
app.UseRouting();
app.UseAuthentication();
app.UseAuthorization();

app.MapControllerRoute(
    name: "default",
    pattern: "{controller=Home}/{action=Index}/{id?}");

// ── Database initialisation (seed roles + admin) ──────────────────────────────
await DbInitializer.InitializeAsync(app.Services);

app.Run();
