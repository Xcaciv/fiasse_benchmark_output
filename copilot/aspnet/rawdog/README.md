# Loose Notes

Loose Notes is an ASP.NET Core MVC note-sharing application with:

- ASP.NET Core Identity authentication
- Entity Framework Core with SQLite
- Private/public notes
- File attachments
- Share links for note access
- 1-5 star ratings with comments
- Search and top-rated note pages
- User profile management
- Admin dashboard with note reassignment and activity logs

## Prerequisites

- .NET SDK 8.0 or newer

## Setup

From the project directory:

```powershell
dotnet restore
dotnet build
```

## Run

```powershell
dotnet run
```

The app uses a local SQLite database file named `app.db`. The database schema is created automatically on startup.

## Default admin account

The application seeds an admin account on startup:

- Username: `admin`
- Email: `admin@loosenotes.local`
- Password: `Admin123!`

You can change these values in `appsettings.json` under `SeedAdmin`.

## Attachments

Uploaded note files are stored under:

```text
App_Data\uploads
```

Supported file types:

- `.pdf`
- `.doc`
- `.docx`
- `.txt`
- `.png`
- `.jpg`
- `.jpeg`

Maximum file size per upload: 10 MB.

## Password reset

Password reset requests generate a local HTML email file instead of sending real email. Files are written to:

```text
App_Data\emails
```

Open the newest file and use the reset link inside it. Reset tokens expire after 1 hour.

## Main routes

- `/` - home page
- `/Home/Search` - note search
- `/Home/TopRated` - top-rated public notes
- `/Notes` - current user's notes
- `/Profile` - profile management
- `/Admin/Dashboard` - admin dashboard
- `/s/{token}` - anonymous shared note access

## Notes

- New notes default to private.
- Public notes are searchable by all users.
- Private notes are only visible to their owner and admins unless shared by link.
- Deleting a note removes its attachments, ratings, and share links.
