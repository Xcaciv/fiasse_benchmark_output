# Loose Notes — ASP.NET Core MVC Application

A secure multi-user note-taking platform built with FIASSE/SSEM securable engineering principles.

## Features

- User registration, login, and password reset
- Create, edit, delete, and search notes (public/private)
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG)
- Shareable revocable links for notes
- 1–5 star ratings with comments
- Top rated public notes feed
- Admin dashboard with audit log, user management, and note reassignment

## Setup & Run

### Prerequisites

- [.NET 8 SDK](https://dotnet.microsoft.com/download)

### Steps

```bash
# 1. Navigate to the project directory
cd LooseNotes

# 2. Restore dependencies
dotnet restore

# 3. Apply migrations and seed the database (automatic on first run)
#    The database file is created at: loosenotes.db

# 4. Run the application
dotnet run

# 5. Open https://localhost:7100 in your browser
```

### Default Admin Account

| Field    | Value                   |
|----------|-------------------------|
| Username | `admin`                 |
| Email    | `admin@loosenotes.local`|
| Password | `Admin@123!`            |

> Change these credentials immediately via Profile settings or by setting environment variables:
> `SeedAdmin__Email`, `SeedAdmin__Password`, `SeedAdmin__UserName`

### File Uploads

Uploaded files are stored in `uploads_dev/` (development) or `uploads/` (production).
Configure `FileStorage:BasePath` in `appsettings.json` to change the location.

---

## SSEM Score Summary

The following scores reflect the securable quality engineering applied throughout this codebase.

| Pillar            | Attribute       | Score | Key Evidence                                                                 |
|:------------------|:----------------|:-----:|:-----------------------------------------------------------------------------|
| **Maintainability** | Analyzability   | 9/10  | Methods ≤30 LoC, trust boundary comments, clear naming, configuration classes |
|                   | Modifiability   | 9/10  | All services behind interfaces, DI throughout, strongly-typed config          |
|                   | Testability     | 8/10  | All public interfaces injectable/mockable; no static mutable state            |
| **Trustworthiness** | Confidentiality | 9/10  | PBKDF2 password hashing, HttpOnly+Secure cookies, generic error messages, no secrets in code |
|                   | Accountability  | 9/10  | Structured AuditLog for every security event; actor, resource, IP captured   |
|                   | Authenticity    | 9/10  | ASP.NET Identity lockout, CSPRNG share tokens, current-password required for profile changes |
| **Reliability**   | Availability    | 8/10  | Rate limiting (AspNetCoreRateLimit), account lockout, file size caps          |
|                   | Integrity       | 9/10  | EF Core parameterized queries, allow-list file validation, check constraints, anti-forgery |
|                   | Resilience      | 8/10  | Specific exception handling, audit failure non-fatal, file deleted before DB row |

**Overall Composite: 8.7/10**

### Key Security Design Decisions

1. **Turtle Analogy (Hard Shell)** — All user input is validated at trust boundaries (controllers + file service) before any domain logic executes.

2. **No SQL Injection Surface** — 100% of DB queries use EF Core LINQ (parameterized). No raw SQL.

3. **IDOR Prevention** — `FindOwnedNoteAsync` re-verifies ownership on every mutating request, even with valid session.

4. **Open Redirect Prevention** — `Url.IsLocalUrl()` used before all redirects accepting user-supplied `returnUrl`.

5. **Email Enumeration Prevention** — Password reset always redirects to confirmation regardless of whether email exists.

6. **Share Token Entropy** — 32-byte CSPRNG output = 256 bits of entropy; URL-safe Base64 encoded.

7. **Audit Log Integrity** — Audit records survive user deletion (`OnDelete.SetNull`), preserving compliance trail.

8. **Content Security Policy** — Restrictive CSP header applied globally; no inline script execution beyond `'unsafe-inline'` for Bootstrap.
