# Loose Notes

A multi-user note-taking web application built with **ASP.NET Core 8 MVC**, Entity Framework Core (SQLite), and ASP.NET Core Identity.

---

## Prerequisites

| Tool | Version |
|------|---------|
| [.NET SDK](https://dotnet.microsoft.com/download) | 8.0 or later |

No external databases, message brokers, or cloud accounts required for local development.

---

## Quick Start

```bash
# 1. Restore packages
cd LooseNotes
dotnet restore

# 2. Apply EF Core migrations and seed the database
#    (also done automatically on first run via db.Database.MigrateAsync())
dotnet ef database update

# 3. Run
dotnet run
```

Browse to **https://localhost:7254** (or the URL shown in the terminal).

### Default Admin Account

| Field    | Value                     |
|----------|---------------------------|
| Username | `admin`                   |
| Email    | `admin@loosenotes.local`  |
| Password | `Admin@Passw0rd!`         |

> **Change this password immediately** in production. Credentials can be overridden via environment variables or `appsettings.Production.json`:
>
> ```json
> {
>   "Seed": {
>     "AdminEmail": "youraddr@example.com",
>     "AdminPassword": "YourStrongP@ss!",
>     "AdminUserName": "youradmin"
>   }
> }
> ```

---

## Configuration

| Key | Default | Description |
|-----|---------|-------------|
| `ConnectionStrings:DefaultConnection` | `Data Source=loosenotes.db` | SQLite path (change to SQL Server / PostgreSQL connection string if needed) |
| `FileStorage:UploadPath` | `uploads` | Directory for uploaded files (relative to content root; not inside wwwroot) |
| `FileStorage:MaxFileSizeBytes` | `10485760` | Max upload size (default 10 MB) |
| `FileStorage:AllowedExtensions` | `.pdf .doc .docx .txt .png .jpg .jpeg` | Allowed upload file types |

### Switching to SQL Server or PostgreSQL

1. Replace the EF SQLite package in `.csproj` with `Microsoft.EntityFrameworkCore.SqlServer` or `Npgsql.EntityFrameworkCore.PostgreSQL`.
2. Update the `UseSqlite(...)` call in `Program.cs` to `UseSqlServer(...)` / `UseNpgsql(...)`.
3. Update the connection string in `appsettings.json`.

---

## Running the EF Core CLI Tools

```bash
# Add a new migration (after model changes)
dotnet ef migrations add <MigrationName>

# Apply pending migrations
dotnet ef database update

# Drop and recreate the database (dev only)
dotnet ef database drop --force && dotnet ef database update
```

---

## Project Structure

```
LooseNotes/
├── Controllers/         # HTTP request handlers
│   ├── AccountController.cs    – registration, login, password reset, profile
│   ├── AdminController.cs      – admin dashboard, user list, note reassignment
│   ├── AttachmentsController.cs – file download + deletion
│   ├── HomeController.cs       – home page, top-rated list
│   ├── NotesController.cs      – note CRUD, file upload, share links
│   ├── RatingsController.cs    – rate / update rating
│   └── ShareController.cs      – anonymous share-link viewer
├── Data/
│   ├── ApplicationDbContext.cs
│   └── DbInitializer.cs        – role + admin user seeding
├── Models/              # EF Core entities
├── Services/            # File storage, email (stub), audit log
├── ViewModels/          # View-specific DTOs (account, notes, admin, ratings)
├── Views/               # Razor templates
├── wwwroot/             # Static assets (CSS, JS)
├── Program.cs           # Application entry-point & DI configuration
├── appsettings.json
└── LooseNotes.csproj
```

---

## SSEM Score Summary

The following table maps each **SSEM (Secure Software Engineering Maturity)** attribute to its implementation in this codebase.

| # | SSEM Attribute | Implementation |
|---|----------------|----------------|
| 1 | **Fail-safe defaults** | Notes default to `IsPublic = false` (private). All controller actions default to deny without explicit `[AllowAnonymous]`. Cookie `SecurePolicy = Always`. Account lockout enabled by default. |
| 2 | **Input validation** | All ViewModels annotated with `[Required]`, `[MaxLength]`, `[Range]`, `[EmailAddress]` etc. File uploads validated by extension white-list and size limit in `LocalFileStorageService`. |
| 3 | **Output encoding** | All Razor views use `@variable` (HTML-encoded by default). No use of `@Html.Raw`. Content-Security-Policy header restricts script sources. `X-Content-Type-Options: nosniff` prevents MIME sniffing. |
| 4 | **Authentication** | ASP.NET Core Identity with PBKDF2 password hashing. Strong password policy (10+ chars, mixed case, digit, special char). Account lockout (5 attempts / 15 min). HttpOnly + Secure + SameSite cookies. |
| 5 | **Authorisation** | Role-based (`User` / `Admin`). `[Authorize]` on all mutation endpoints. Ownership checked server-side before edit/delete (not just at UI level). Private notes enforce access control in `Details` action. |
| 6 | **CSRF protection** | Global `AutoValidateAntiforgeryTokenAttribute` filter. Anti-forgery cookie is HttpOnly + Secure + SameSite=Strict. All state-changing forms include `@Html.AntiForgeryToken()`. |
| 7 | **SQL injection prevention** | All queries use EF Core parameterized LINQ — no raw SQL string concatenation. |
| 8 | **File upload security** | Extension white-list (deny by default). Size limit enforced. Stored filename is always a GUID (never user-supplied). Upload directory is outside `wwwroot`. Path traversal guard in `BuildSafePath`. |
| 9 | **Secure share links** | Tokens are 256-bit cryptographically random hex strings (`RandomNumberGenerator`). Token lookup is indexed. Tokens can be revoked individually. |
| 10 | **Audit logging** | `AuditService` records login success/failure, lockout, registration, logout, password change/reset, note CRUD, file operations, admin actions. Passwords and raw tokens are never logged. |
| 11 | **Security headers** | `X-Content-Type-Options`, `X-Frame-Options: DENY`, `X-XSS-Protection`, `Referrer-Policy`, `Permissions-Policy`, `Content-Security-Policy` applied globally via middleware. HSTS enabled in production. |
| 12 | **Account enumeration mitigation** | `ForgotPassword` always shows the same confirmation page regardless of whether the email exists. Login error message is deliberately generic. |
| 13 | **Open redirect prevention** | `returnUrl` validated with `Url.IsLocalUrl()` before redirect. |
| 14 | **Sensitive data protection** | `.gitignore` excludes database, upload directory, log files, and `secrets.json`. Seed credentials configurable via environment variables. |

---

## Production Checklist

- [ ] Replace `LoggingEmailService` with a real SMTP / transactional-email provider.
- [ ] Set `Seed:AdminPassword` from an environment variable or secrets manager.
- [ ] Switch the database to SQL Server or PostgreSQL.
- [ ] Configure HTTPS certificate and `UseHsts`.
- [ ] Enable `RequireConfirmedAccount = true` and wire up email confirmation.
- [ ] Review and tighten the Content-Security-Policy nonce/hash for any inline scripts.
- [ ] Set up log shipping (e.g. Serilog → Seq / Elastic / Azure Monitor).
- [ ] Schedule regular database backups.
