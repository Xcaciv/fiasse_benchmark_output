# Loose Notes

A multi-user note-taking web application built with ASP.NET Core MVC.

## Features

- User registration, login, and password reset
- Create, edit, delete, and search notes
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Public/private note visibility
- Share notes via unique share links
- Rate notes (1–5 stars with optional comment)
- Top Rated notes page
- Admin dashboard with user management and note reassignment

## Prerequisites

- [.NET 8 SDK](https://dotnet.microsoft.com/download/dotnet/8)

## Setup & Run

```bash
# 1. Restore packages and run
cd /path/to/project
dotnet run
```

The app runs on `http://localhost:5000` by default. The SQLite database
(`loosenotes.db`) and migrations are applied automatically on first run.

## Default Admin Account

| Username | Password  |
|----------|-----------|
| admin    | Admin@123 |

## Project Structure

```
Controllers/    - MVC controllers
Data/           - EF Core DbContext and seed data
Migrations/     - Database migrations
Models/         - Entity models
Services/       - File storage and email services
ViewModels/     - View-specific data models
Views/          - Razor views
uploads/        - Uploaded file storage (created at runtime)
```

## Configuration

Edit `appsettings.json` to change:
- `ConnectionStrings:DefaultConnection` — SQLite path (default: `loosenotes.db`)
- `FileStorage:Path` — Upload directory (default: `uploads`)
