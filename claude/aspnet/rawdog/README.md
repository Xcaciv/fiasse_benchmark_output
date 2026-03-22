# Loose Notes

A multi-user note-taking web application built with ASP.NET Core MVC, Entity Framework Core, and ASP.NET Core Identity.

## Features

- User registration, login, and password reset
- Create, edit, delete, and search notes (public/private)
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Note sharing via unique share links
- 1–5 star ratings with comments
- Top Rated notes page
- Admin dashboard with user management and note reassignment

## Prerequisites

- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8)

No additional database software is needed — the app uses SQLite by default.

## Setup and Run

### 1. Restore packages

```bash
dotnet restore
```

### 2. Apply database migrations

```bash
dotnet ef database update
```

> If `dotnet ef` is not installed: `dotnet tool install --global dotnet-ef`

Alternatively, the app will apply migrations automatically on startup.

### 3. Run the application

```bash
dotnet run
```

Then open your browser at `https://localhost:5001` (or the URL shown in the terminal).

## Default Admin Account

On first run, a default admin account is seeded:

| Field    | Value                    |
|----------|--------------------------|
| Username | `admin`                  |
| Email    | `admin@loosenotes.local` |
| Password | `Admin1234!`             |

**Change this password immediately after your first login.**

## Configuration

Edit `appsettings.json` to adjust:

| Setting | Description | Default |
|---|---|---|
| `ConnectionStrings:DefaultConnection` | SQLite connection string | `Data Source=loosenotes.db` |
| `FileStorage:UploadPath` | Directory for uploaded files | `wwwroot/uploads` |
| `FileStorage:MaxFileSizeBytes` | Max upload size in bytes | `10485760` (10 MB) |
| `FileStorage:AllowedExtensions` | Allowed file extensions | pdf, doc, docx, txt, png, jpg, jpeg |

### Email (Password Reset)

By default, password reset emails are **logged to the console** rather than sent. To enable real email delivery, replace `LoggingEmailService` in `Services/` with an SMTP or SendGrid implementation and register it in `Program.cs`.

### Using a Different Database

To use SQL Server instead of SQLite:

1. Replace the SQLite NuGet package with `Microsoft.EntityFrameworkCore.SqlServer`
2. Update `Program.cs`: change `UseSqlite` to `UseSqlServer`
3. Update the connection string in `appsettings.json`

## Project Structure

```
LooseNotes/
├── Controllers/        # MVC controllers
├── Data/               # DbContext and seed data
├── Models/             # Entity models
├── Services/           # File storage and email services
├── ViewModels/         # View models
├── Views/              # Razor views
│   ├── Account/        # Auth views
│   ├── Admin/          # Admin views
│   ├── Notes/          # Note CRUD views
│   ├── Profile/        # Profile edit
│   ├── Share/          # Shared note view
│   └── Shared/         # Layout and partials
└── wwwroot/
    └── uploads/        # Uploaded files (auto-created)
```

## Running Migrations (Development)

If you change the models:

```bash
dotnet ef migrations add <MigrationName>
dotnet ef database update
```
