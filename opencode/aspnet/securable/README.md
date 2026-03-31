# Loose Notes - ASP.NET Core MVC Application

A multi-user note-taking platform with sharing, rating, and admin capabilities.

## Prerequisites

- .NET 8.0 SDK or later
- SQLite (included)

## Setup and Run

1. **Restore dependencies:**
   ```bash
   dotnet restore
   ```

2. **Run the application:**
   ```bash
   dotnet run
   ```

3. **Access the application:**
   - Open browser to `https://localhost:7000`
   - Default admin: `admin@loosenotes.local` / `Admin123!`

## Features

- User registration and authentication
- Create, edit, delete notes
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Public/Private note visibility
- Share links for notes
- Rating system (1-5 stars with comments)
- Search functionality
- Admin dashboard with user management
- Note ownership reassignment

---

## SSEM Attribute Coverage Summary

### Maintainability (33%)

| Attribute | Implementation |
|-----------|----------------|
| **Analyzability** | Clear separation of concerns with Models, Services, Controllers. Meaningful naming conventions. Structured logging throughout. |
| **Modifiability** | DI-based service architecture allows easy component replacement. Configuration externalized to appsettings.json. |
| **Testability** | Service interfaces enable unit testing with mocks. DbContext configured for testing. |

### Trustworthiness (34%)

| Attribute | Implementation |
|-----------|----------------|
| **Confidentiality** | ASP.NET Core Identity with PBKDF2 password hashing. Role-based authorization with [Authorize] attributes. Ownership verification before operations. |
| **Accountability** | Activity logging for admin actions. Login/logout logging. Failed attempt tracking. Audit trail for note ownership changes. |
| **Authenticity** | Anti-forgery tokens on all forms. Input validation with model annotations. SQL injection prevention via Entity Framework parameterized queries. |

### Reliability (33%)

| Attribute | Implementation |
|-----------|----------------|
| **Availability** | Proper error handling with try-catch in services. Global exception handler in production. Graceful degradation for missing resources. |
| **Integrity** | Foreign key constraints in database. Cascading deletes for related entities. Race condition handling in rating system. |
| **Resilience** | File validation before processing. Token-based password reset with expiration concept. Account lockout after failed attempts. |

### Security Controls

- **XSS Prevention**: Razor views auto-encode output
- **CSRF Protection**: `@Html.AntiForgeryToken()` on all state-changing forms
- **SQL Injection**: Entity Framework Core with parameterized queries
- **Password Security**: ASP.NET Core Identity default PBKDF2 with salt
- **Authorization**: Role-based access (User/Admin) with ownership checks
- **File Upload**: Extension validation, size limits, unique storage names
- **Logging**: Structured logging without sensitive data exposure

## Project Structure

```
LooseNotes/
├── Controllers/       # MVC Controllers
├── Data/             # DbContext and seed data
├── Models/           # Entity classes and view models
├── Services/         # Business logic (NoteService, FileService, etc.)
├── Views/            # Razor views
├── wwwroot/          # Static assets (CSS, JS)
└── appsettings.json  # Configuration
```
