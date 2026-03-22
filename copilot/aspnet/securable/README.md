# Loose Notes Web Application

Loose Notes is an ASP.NET Core MVC application for secure, multi-user note taking. It supports ASP.NET Core Identity authentication, note creation and editing, controlled public/private visibility, local attachment storage, share links, ratings, search, profile management, and admin reassignment tools.

## Setup and run

1. Install the .NET 8 SDK.
2. From the project directory, restore and build the app:
   ```powershell
   dotnet restore
   dotnet build
   ```
3. Optionally configure a bootstrap admin account in `appsettings.json` or with environment variables:
   ```powershell
   $env:BootstrapAdmin__UserName = "admin"
   $env:BootstrapAdmin__Email = "admin@example.com"
   $env:BootstrapAdmin__Password = "ChangeMe123!"
   ```
4. Run the application:
   ```powershell
   dotnet run
   ```
5. Open the URL shown in the console.
6. Password reset emails are written to `App_Data\outbox` as HTML files.

## Notes on storage and security

- SQLite is used by default with the database file at `App_Data\loosenotes.db`.
- Attachments are stored outside `wwwroot` in `App_Data\attachments` and are only served through controller actions after authorization checks or share-link validation.
- Share links are generated from cryptographically random tokens. The database stores a token hash for lookup and a protected token value so active links can be shown again in the UI.
- All state-changing MVC form posts are protected by automatic anti-forgery validation.

## Feature coverage

The generated application includes the following PRD features:

- User registration, login, logout, password reset, and profile management using ASP.NET Core Identity.
- Note creation, editing, deletion, public/private visibility, search, and top-rated pages.
- Multiple attachments per note with type and size validation.
- Share-link generation, regeneration, revocation, and anonymous shared-note access.
- Ratings with 1-5 stars plus optional comments, including owner/admin ratings views.
- Admin dashboard with user search, counts, recent activity, and note ownership reassignment.

## SSEM attribute coverage summary

This implementation addresses nine core security attributes as follows:

1. **Authentication**: ASP.NET Core Identity manages registration, sign-in, cookie sessions, password hashing, lockout, and password reset tokens.
2. **Authorization**: Controller actions enforce `[Authorize]`, role checks, and ownership checks before edit, delete, sharing, and admin operations.
3. **Confidentiality**: Private notes remain owner/admin-only, attachments are stored outside the web root, and share links use high-entropy tokens.
4. **Integrity**: EF Core relationships, validation attributes, anti-forgery protection, and constrained file handling help prevent tampering.
5. **Availability**: The app uses lightweight SQLite persistence, bounded file uploads, and predictable local storage paths to keep the service operational.
6. **Accountability**: Authentication, note, sharing, and admin events are written to the `ActivityLogs` table for traceability.
7. **Auditability**: The admin dashboard surfaces recent activity and user/note counts so privileged users can review important actions.
8. **Privacy**: Search filters exclude private notes owned by other users, and logs intentionally avoid secrets such as passwords or reset tokens.
9. **Resilience**: Secure defaults, input validation, lockout settings, and explicit revocation of share links reduce common misuse and abuse cases.
