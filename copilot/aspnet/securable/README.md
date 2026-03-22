# LooseNotes

A securable ASP.NET Core 8 MVC note-taking application, built with FIASSE/SSEM engineering constraints throughout.

## Prerequisites

- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- No additional tools required (SQLite is embedded)

## Setup

```bash
# 1. Clone / navigate to the project directory
cd path/to/securable

# 2. Restore NuGet packages
dotnet restore

# 3. Create the database and apply migrations
dotnet ef database update

# 4. Run the application
dotnet run
```

The app starts at `https://localhost:5001` (or `http://localhost:5000`).

## Default Admin Credentials

The admin user is seeded automatically on first startup.

**Set the admin password via environment variable (recommended):**

```bash
# Linux / macOS
export ADMIN_PASSWORD="MyStr0ng!Pass"
dotnet run

# Windows PowerShell
$env:ADMIN_PASSWORD = "MyStr0ng!Pass"
dotnet run
```

If `ADMIN_PASSWORD` is not set, a random password is generated and printed to the console at startup (`[STARTUP] Generated admin password: ...`). This is intentionally visible only in the console — it is **not** written to structured log sinks.

Default admin username: `admin` (configurable via `Identity:DefaultAdminUsername` in `appsettings.json`)

## Project Structure

```
LooseNotes/
├── Controllers/         # MVC controllers — auth, notes, admin, share, ratings, attachments
├── Data/                # EF Core DbContext (IdentityDbContext<ApplicationUser>)
├── Models/              # Domain models (Note, Attachment, Rating, ShareLink, ActivityLog)
├── Services/
│   ├── Interfaces/      # INoteService, IFileStorageService, IShareLinkService, IAuditService, IEmailService
│   └── *.cs             # Implementations (all injected via DI)
├── ViewModels/          # Per-feature view models (Account/, Notes/, Admin/)
├── Views/               # Razor views organized by controller
├── wwwroot/             # Static assets (CSS, JS)
└── Uploads/             # File attachments (created at runtime, outside wwwroot)
```

## SSEM Attribute Coverage

| Attribute | Implementation |
|---|---|
| **Analyzability** | Methods ≤30 LoC, helper methods extracted, cyclomatic complexity <10, trust-boundary comments |
| **Modifiability** | All services behind interfaces, registered with DI, no static mutable state |
| **Testability** | All public interfaces are `interface`-typed and injectable/mockable; no `new` in business logic |
| **Confidentiality** | Passwords/tokens never logged; error views show friendly messages; files stored outside wwwroot; anti-email-enumeration in ForgotPassword |
| **Accountability** | Structured audit log (`ActivityLog`) for: registration, login, logout, password reset, note CRUD, admin actions |
| **Authenticity** | ASP.NET Core Identity with lockout; anti-forgery tokens on all POST forms; `[Authorize]` and `[Authorize(Roles="Admin")]`; share tokens are opaque GUIDs |
| **Availability** | Rate limiting (100 req/min per IP); file size limits (10 MB); result set caps (search: 100, top-rated: 20); audit failures don't crash callers |
| **Integrity** | Data Annotations validation at every trust boundary; EF Core parameterized queries; ownership verified before every mutation; allowed file extension whitelist |
| **Resilience** | Specific exception handling (`IOException`, `DbUpdateException`, `ArgumentException`); `ArgumentNullException.ThrowIfNull` throughout; seed failures don't crash startup |

## Security Notes

- Files are stored in `Uploads/` (outside `wwwroot`) and served only through `AttachmentsController`, enforcing access control on every download
- Share tokens use `Guid.NewGuid().ToString("N")` (32 opaque hex characters)
- Cookie auth: `HttpOnly=true`, `Secure=Always`, `SameSite=Strict`, sliding 30-min expiration
- Identity: min 8 chars, requires digit + uppercase + special char; 5-attempt lockout for 15 min
- Admin password sourced from `ADMIN_PASSWORD` env var — never hardcoded in source or config
