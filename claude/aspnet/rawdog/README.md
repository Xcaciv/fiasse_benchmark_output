# LooseNotes

A multi-user web platform for creating, managing, sharing, and rating text notes.

## Prerequisites

- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8.0)

## Setup & Run

1. **Restore packages**

   ```bash
   dotnet restore
   ```

2. **Run the application**

   ```bash
   dotnet run
   ```

   The application will create the SQLite database (`loosenotes.db`) and seed default accounts on first startup.

3. **Open in browser**

   Navigate to `http://localhost:5000` (or the port shown in the terminal output).

## Default Accounts

Seeded at startup from `appsettings.json`:

| Username | Password     | Role  |
|----------|-------------|-------|
| admin    | admin123    | Admin |
| alice    | password123 | User  |
| bob      | bobpass456  | User  |

## Project Structure

```
LooseNotes/
├── Controllers/        # MVC controllers
├── Data/               # EF Core DbContext and seed data
├── Migrations/         # EF Core migrations
├── Models/             # Domain models
├── Services/           # Email and file storage services
├── ViewModels/         # View model classes
├── Views/              # Razor views
│   ├── Account/        # Login, register, password recovery
│   ├── Admin/          # Admin dashboard
│   ├── Home/           # Home page and diagnostics
│   ├── Notes/          # Note CRUD, search, import/export
│   ├── Profile/        # User profile
│   ├── Share/          # Public share view
│   └── Shared/         # Layout and partials
├── wwwroot/
│   └── attachments/    # Uploaded file storage (web-accessible)
├── appsettings.json
└── Program.cs
```

## Features

- **User accounts**: Registration, login, password recovery via security question
- **Notes**: Create, edit, delete, search (public/private visibility)
- **File attachments**: Upload files to notes; download via direct link
- **Sharing**: Generate share links for unauthenticated access
- **Ratings**: Rate and comment on notes (1–5 stars)
- **Top Rated**: Browse highest-rated public notes
- **Import/Export**: ZIP archive with JSON manifest and attachments
- **XML processing**: Batch data migration endpoint
- **Admin dashboard**: User management, note reassignment, command execution, logs
- **Profile management**: Update username, email, password, security question
- **Diagnostics**: HTTP request header viewer

## Database

The application uses SQLite via Entity Framework Core. The database file (`loosenotes.db`) is created automatically on first run using `EnsureCreated()`.

To use EF Core migrations instead:

```bash
dotnet ef database update
```
