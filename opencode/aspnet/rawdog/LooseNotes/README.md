# LooseNotes - Multi-User Note-Taking Web Application

A complete ASP.NET Core MVC application for creating, managing, sharing, and rating notes.

## Features

- **User Authentication**: Register, login, password reset, and profile management
- **Note Management**: Create, edit, delete, and view notes
- **File Attachments**: Upload and attach files to notes (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- **Public/Private Notes**: Control note visibility
- **Note Sharing**: Generate share links to share notes with anyone
- **Rating System**: Rate notes with 1-5 stars and optional comments
- **Search**: Search notes by title or content
- **Top Rated**: View highest-rated public notes
- **Admin Dashboard**: User management, note reassignment, and activity logs
- **Role-Based Access**: User and Admin roles

## Prerequisites

- .NET 8.0 SDK or later
- A modern web browser

## Setup and Installation

### 1. Clone or Extract the Project

```bash
cd LooseNotes
```

### 2. Restore Dependencies

```bash
dotnet restore
```

### 3. Run the Application

```bash
dotnet run
```

The application will start and create the SQLite database automatically.

### 4. Access the Application

Open your browser and navigate to:
- `https://localhost:5001` or `http://localhost:5000`

## Default Admin Account

On first run, an admin account is automatically created:

- **Username**: `admin`
- **Email**: `admin@loosenotes.local`
- **Password**: `Admin123!`

You can use these credentials to access the admin dashboard.

## Project Structure

```
LooseNotes/
├── Controllers/          # MVC Controllers
│   ├── AccountController.cs
│   ├── AdminController.cs
│   ├── HomeController.cs
│   ├── NotesController.cs
│   └── SearchController.cs
├── Data/                 # Database context
├── Models/               # Entity models and view models
├── Services/             # Business logic services
├── Views/                # Razor views
│   ├── Account/         # Authentication views
│   ├── Admin/           # Admin views
│   ├── Home/            # Home page views
│   ├── Notes/           # Note management views
│   ├── Search/          # Search views
│   └── Shared/          # Shared layouts and partials
├── wwwroot/             # Static files
│   ├── css/            # Stylesheets
│   ├── js/             # JavaScript files
│   └── uploads/        # Uploaded files storage
├── Program.cs           # Application entry point
└── appsettings.json    # Configuration
```

## Database

The application uses SQLite by default. The database file `loosenotes.db` is created automatically in the project root on first run.

To use a different database, update the connection string in `appsettings.json`:

```json
{
  "ConnectionStrings": {
    "DefaultConnection": "Data Source=loosenotes.db"
  }
}
```

Supported providers:
- SQLite (default)
- SQL Server
- PostgreSQL
- MySQL

## Security Features

- Password hashing using ASP.NET Core Identity (PBKDF2)
- Cookie-based authentication
- Role-based authorization
- Anti-forgery tokens on all forms
- SQL injection protection via Entity Framework
- XSS protection via Razor output encoding

## API Routes

### Public Routes
- `/` - Home page
- `/Home/Privacy` - Privacy policy
- `/Search` - Search notes
- `/Notes/Details/{id}` - View note
- `/Notes/TopRated` - Top rated notes
- `/Notes/Download/{id}` - Download attachment

### Authenticated Routes
- `/Account/Profile` - User profile
- `/Account/ChangePassword` - Change password
- `/Notes` - List user's notes
- `/Notes/Create` - Create new note
- `/Notes/Edit/{id}` - Edit note
- `/Notes/Delete/{id}` - Delete note
- `/Notes/Share/{id}` - Manage share links

### Admin Routes
- `/Admin` - Admin dashboard
- `/Admin/Users` - User management
- `/Admin/ReassignNote/{id}` - Reassign note ownership
- `/Admin/ActivityLogs` - View activity logs

## Configuration

### File Upload Settings

Configure in `appsettings.json`:

```json
{
  "FileUpload": {
    "MaxFileSizeMB": 10,
    "AllowedExtensions": [".pdf", ".doc", ".docx", ".txt", ".png", ".jpg", ".jpeg"],
    "UploadPath": "wwwroot/uploads"
  }
}
```

### Logging

Application logs are written to the console and file (in production). Configure log levels in `appsettings.json`:

```json
{
  "Logging": {
    "LogLevel": {
      "Default": "Information",
      "Microsoft.AspNetCore": "Warning"
    }
  }
}
```

## Development

### Build

```bash
dotnet build
```

### Run Tests

```bash
dotnet test
```

### Publish

```bash
dotnet publish -c Release -o ./publish
```

## Technologies Used

- ASP.NET Core 8.0 MVC
- Entity Framework Core 8.0
- SQLite Database
- ASP.NET Core Identity
- Bootstrap 5.3
- jQuery 3.7

## License

This project is provided as-is for educational and personal use.
