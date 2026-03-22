# Loose Notes

A multi-user note-taking web application built with ASP.NET Core MVC.

## Features

- User registration and authentication (ASP.NET Core Identity)
- Create, edit, delete private/public notes
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Note sharing via generated share links
- 1–5 star ratings with optional comments
- Full-text note search
- Top-rated notes page
- Admin dashboard (user list, note reassignment)
- Password reset (logged to console in dev)

## Prerequisites

- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8)

## Setup & Run

```bash
# Restore packages
dotnet restore

# Run the application (applies migrations and seeds admin user automatically)
dotnet run
```

The app will be available at `https://localhost:5001` (or the port shown in the console).

### Default Admin Account

| Field    | Value              |
|----------|--------------------|
| Username | `admin`            |
| Password | `Admin@123456`     |

Change this password immediately after first login.

## Configuration

Edit `appsettings.json` to change:

| Key                             | Default              | Description                  |
|---------------------------------|----------------------|------------------------------|
| `ConnectionStrings:DefaultConnection` | `Data Source=loosenotes.db` | SQLite connection string |
| `FileStorage:UploadPath`        | `wwwroot/uploads`    | Uploaded file directory      |

## Database

The app uses SQLite by default. To switch to SQL Server or PostgreSQL:

1. Install the appropriate EF Core provider package.
2. Update `Program.cs` to use `UseSqlServer(...)` or `UseNpgsql(...)`.
3. Update the connection string in `appsettings.json`.
4. Run `dotnet ef database update` (or let the app auto-migrate on startup).

## Email (Password Reset)

Password reset links are written to the application log in development. To send real emails, implement `IEmailService` with an SMTP or transactional email provider and register it in `Program.cs`.

## Project Structure

```
LooseNotes/
├── Controllers/        # MVC controllers
├── Data/               # EF Core DbContext and seed data
├── Migrations/         # EF Core migrations
├── Models/             # Entity models
├── Services/           # File storage and email services
├── ViewModels/         # View-specific models
├── Views/              # Razor views
│   ├── Account/
│   ├── Admin/
│   ├── Home/
│   ├── Notes/
│   ├── Profile/
│   ├── Share/
│   └── Shared/
├── wwwroot/uploads/    # Uploaded files (created at runtime)
├── appsettings.json
└── Program.cs
```
