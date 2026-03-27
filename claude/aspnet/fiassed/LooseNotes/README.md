# Loose Notes — ASP.NET Core MVC Application

A secure, multi-user note-taking web application built with ASP.NET Core 8.0 MVC, applying
FIASSE/SSEM securable engineering principles and OWASP ASVS Level 2 requirements throughout.

---

## Setup and Run Instructions

### Prerequisites

- [.NET 8.0 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)
- Git (to clone the repo)

### Quick Start

```bash
# 1. Navigate to the project directory
cd LooseNotes

# 2. Restore dependencies
dotnet restore

# 3. Apply database migrations (creates loosenotes.db)
dotnet ef database update

# 4. (Optional) Set admin credentials via environment variables
#    Defaults: admin / Admin@LooseNotes1 / admin@localhost
export ADMIN_USERNAME=admin
export ADMIN_EMAIL=admin@localhost
export ADMIN_PASSWORD=YourSecureAdminPassword1!

# 5. Run the application
dotnet run
```

The application will start at `https://localhost:5001` and `http://localhost:5000`.

### Running in Development

```bash
dotnet run --environment Development
```

Development mode enables detailed logging. The `LoggingEmailService` will print password reset
links to the console instead of sending emails.

### Database Migrations (if schema changes)

```bash
dotnet ef migrations add <MigrationName>
dotnet ef database update
```

### Production Notes

- Replace `LoggingEmailService` with a real SMTP/transactional email implementation.
- Set `ADMIN_PASSWORD` via a secrets manager — never commit credentials.
- Configure TLS in your reverse proxy (nginx, IIS, Azure App Service).
- Set `ASPNETCORE_ENVIRONMENT=Production` to disable developer error pages.
- Review `appsettings.json` rate-limit thresholds for your expected traffic.
- Audit log retention: implement a scheduled job to archive or prune `AuditLogs` table entries
  beyond your retention policy window.

---

## Project Structure

```
LooseNotes/
├── Configuration/          # Externalized options (SecurityOptions, FileStorageOptions, PasswordPolicyOptions)
├── Controllers/            # MVC controllers (Account, Notes, Attachments, Share, Admin, Profile, Home)
├── Data/                   # EF Core DbContext and DbInitializer
├── Migrations/             # EF Core migration files
├── Models/                 # Entity models (ApplicationUser, Note, Attachment, Rating, ShareLink, AuditLog)
├── Resources/              # Embedded resources (CommonPasswords.txt)
├── Services/               # Service interfaces and implementations
├── ViewModels/             # Typed view models for each feature
├── Views/                  # Razor views organized by controller
├── wwwroot/                # Static assets (CSS, JS)
├── appsettings.json        # Application configuration
└── Program.cs              # Application bootstrap and middleware pipeline
```

---

## SSEM Attribute Coverage Summary

The nine SSEM attributes are addressed across the codebase as follows:

### Maintainability Pillar

#### Analyzability
- Controller actions are single-purpose with ≤ 30 lines of core logic; complex flows are
  delegated to named service methods (e.g., `StoreFileAsync`, `LogAsync`).
- `AuditEventTypes` static class provides named constants for all event types, making log
  filtering transparent without string hunting.
- Trust boundaries are explicitly identified: every controller extracts `UserId` from
  `User.GetUserId()` and never from request body, with a comment explaining why.
- `ApplicationDbContext.OnModelCreating` is broken into named configuration methods
  (`ConfigureNoteEntity`, `ConfigureAttachmentEntity`, etc.) for readability.

#### Modifiability
- All security-sensitive thresholds (session timeout, rate limits, file size, password policy)
  are externalized in `appsettings.json` via typed options classes (`SecurityOptions`,
  `FileStorageOptions`, `PasswordPolicyOptions`). Changing policy requires no code deployment.
- File type allowlist is configuration-driven; adding a new permitted type requires only
  a configuration entry, not a code change.
- The rate limiter in `Program.cs` reads all thresholds from configuration, enabling
  operational tuning without redeployment.
- `IEmailService`, `IFileStorageService`, `IShareTokenService`, `IPasswordValidationService`
  are abstractions; production replacements require only a DI registration change.

#### Testability
- All services depend on injected interfaces, making them replaceable with test doubles.
- `IPasswordValidationService` is independently testable: each validation path (length, common
  password check) is a distinct code path with a clear return contract.
- `IFileStorageService` separates validation from storage, so validation unit tests do not
  require a real filesystem.
- `IAuditService` is injected everywhere, enabling audit log assertions in integration tests.
- Ownership checks in controllers (`note.UserId != userId`) are explicit and testable via
  integration tests that submit requests with mismatched session users.

---

### Trustworthiness Pillar

#### Confidentiality
- Passwords are never stored, logged, or echoed. ASP.NET Core Identity uses PBKDF2 hashing.
- Session tokens are transmitted only via `HttpOnly` + `Secure` + `SameSite=Strict` cookies.
- Audit log entries record events without storing the values that changed (e.g., "email
  changed" is logged, the new email value is not).
- Share link tokens are not logged in plaintext; only the link ID is logged.
- Admin user list responses are shaped by `UserSummaryViewModel`, which excludes
  `PasswordHash`, `SecurityStamp`, and reset tokens by design.
- Rater email addresses are excluded from all rating list responses via `RatingViewModel`.
- The `LocalFileStorageService` stores files with server-generated UUID names; original
  filenames are metadata only and never appear in file system paths.

#### Accountability
- `IAuditService` writes structured log entries for every security-sensitive action:
  registration, login (success/failure), logout, password reset (request/complete/token reuse),
  note CRUD, visibility changes, file upload/download/delete, share link create/revoke/access,
  rating create/edit, admin actions (dashboard view, user search, note reassignment, session
  termination).
- `AuditEventTypes` constants provide a complete inventory of logged events (ASVS V16.1.1).
- All audit entries include: `timestamp` (UTC), `eventType`, `userId`, `username`, `sourceIp`,
  `outcome`, and `resourceType`/`resourceId` where applicable (ASVS V16.2.1).
- Serilog writes structured JSON logs to rolling daily files for operational retention.

#### Authenticity
- ASP.NET Core Identity manages session token lifecycle. `SignInManager.PasswordSignInAsync`
  regenerates the session token on every successful authentication (ASVS V7.2.4), preventing
  session fixation.
- `SecurityStamp` invalidation is used after password reset and password change to terminate
  all active sessions by invalidating the security stamp validation (ASVS V7.4.3).
- Password reset tokens are generated by Identity's `GeneratePasswordResetTokenAsync`
  (CSPRNG-backed, >128 bits entropy, ASVS V7.2.3).
- Share link tokens use `RandomNumberGenerator.GetBytes(32)` (256 bits CSPRNG entropy,
  URL-safe base64, ASVS V7.2.3).
- `UserId` is always extracted from `HttpContext.User` (server-issued principal), never from
  request body parameters (Derived Integrity Principle, ASVS V8.3.1).

---

### Reliability Pillar

#### Availability
- Rate limiting is applied to all high-risk endpoints: login, registration, password reset,
  search, file upload, top-rated, share link view, and rating submission (ASVS V2.4.1).
- All rate-limit thresholds are configuration-driven, enabling adjustment without redeployment.
- Server-side field length limits on title (500), content (100,000), search query (200), and
  rating comment (1,000) prevent storage exhaustion and oversized payload attacks.
- Per-user file storage quota (count and total bytes) enforced before accepting uploads.
- Lockout policy configured: 10 failed attempts triggers a 15-minute lockout (non-permanent).
- Request body size limit is set to `MaxFileSizeBytes + overhead` to reject oversized uploads
  before the controller action is reached (ASVS V5.2.1).

#### Integrity
- Anti-forgery tokens are enforced globally via `AutoValidateAntiforgeryTokenAttribute` on all
  controllers (GSR-03, ASVS V3.5).
- Visibility defaults to `false` (private) at the model level and is set server-side at note
  creation; client-supplied defaults are ignored (Derived Integrity Principle, ASVS V8.2.2).
- `UpdatedAt` timestamps are set server-side at save time; client-supplied values are not
  accepted (Derived Integrity Principle).
- Rating range (1–5) is validated server-side in the controller before any database operation
  (ASVS V2.2.1).
- Average ratings are computed server-side after each rating save; client-supplied averages
  are never accepted.
- Magic-byte validation in `LocalFileStorageService` confirms file content matches declared
  type, not just extension (ASVS V5.2.2).
- Razor views use default HTML encoding for all user-supplied content; `@Html.Raw` is not
  used for user data (GSR-07, ASVS V3.2.2).
- Parameterized queries via Entity Framework Core prevent SQL injection throughout.

#### Resilience
- EF Core cascade delete configuration ensures note deletion atomically removes attachments,
  ratings, and share links in a single database operation (ASVS V2.3.3).
- `AuditService.LogAsync` catches write failures and emits a fallback structured log entry
  rather than propagating the exception to the primary operation.
- File storage service validates all conditions (size, type, quota) before writing to disk,
  preventing partial upload states.
- Password reset and credential change operations are atomic: if the database update fails,
  the original credential remains valid and the token is not consumed.
- Session termination after password reset/change uses `UpdateSecurityStampAsync`, which
  invalidates all existing session cookies without requiring explicit session store enumeration.
- Security headers middleware is applied to every response regardless of route or content
  type (GSR-02).

---

## Security Controls Summary (ASVS Level 2)

| Control Area | Implementation |
|---|---|
| Password storage | ASP.NET Core Identity PBKDF2 |
| Password policy | Min 8 chars, max 128, common-password check, no composition rules (V6.2) |
| Session tokens | CSPRNG via Identity; regenerated on login; Secure+HttpOnly+SameSite=Strict (V7.2, V3.3) |
| Session timeout | 30 min inactivity (configurable); absolute 8h lifetime via `ExpireTimeSpan` (V7.3) |
| CSRF protection | Global `AutoValidateAntiforgeryTokenAttribute` (V3.5) |
| Rate limiting | Per-endpoint fixed-window limiters on all sensitive endpoints (V2.4) |
| Authorization | Server-side ownership checks on all mutations; class-level `[Authorize(Roles="Admin")]` (V8.2) |
| Input validation | Server-side via model annotations and explicit service checks (V2.2) |
| Output encoding | Razor default HTML encoding on all user content (V3.2) |
| File uploads | Magic-byte validation, UUID storage names, Content-Disposition: attachment (V5.2, V5.3) |
| Share tokens | 256-bit CSPRNG URL-safe base64, server-side revocation (V7.2) |
| Audit logging | Structured Serilog + database entries for all security events (V16.2) |
| Security headers | CSP, X-Content-Type-Options, Referrer-Policy, X-Frame-Options (V3.4) |
| Error handling | Generic error page; no stack traces to clients (V16 / GSR-08) |
| Transport security | HTTPS redirect + HSTS enforced in production (V12.1) |

---

## Open Gaps (from PRD Section 4)

| Gap | Status |
|---|---|
| G-01: Share link behavior on note privacy change | Share links remain valid after note is made private; only explicit revocation denies access. Documented by design. |
| G-02: File download authentication via share link | Implemented: `ShareController.DownloadAttachment` validates share token before serving file. |
| G-03: Email confirmation at registration | Not implemented at L2 baseline. `LoggingEmailService` placeholder in place for production upgrade. |
| G-04: Audit log write protection | Logs written to database and Serilog file sink. Append-only enforcement requires infrastructure-level configuration (outside application scope). |
| G-05: Concurrent session limits | Not enforced; security stamp invalidation provides remediation capability. |
| G-06: Current password required for email change | Not enforced for email changes (only for password changes). Noted as residual risk. |
| G-07: File content scanning | Magic-byte validation only; no anti-malware integration. Residual risk accepted at L2. |
| G-08: Search implementation | EF Core `LIKE` with parameterized queries. Full-text index can be added as a migration. |
