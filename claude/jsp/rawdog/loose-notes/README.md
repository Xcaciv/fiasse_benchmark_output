# Loose Notes

A multi-user note-taking web application built with Java Servlets, JSP, and SQLite.

## Features

- **User Authentication**: Registration, login, logout, and password reset
- **Note Management**: Create, read, update, and delete notes
- **Privacy Control**: Notes can be public or private
- **File Attachments**: Upload and download files attached to notes
- **Note Sharing**: Generate shareable links (no login required to view)
- **Ratings & Comments**: Rate notes (1-5 stars) with optional comments
- **Search**: Search through public and your own notes
- **Top Rated**: Browse the highest-rated public notes
- **User Profiles**: View and update email, change password
- **Admin Dashboard**: User management, note reassignment, activity log

## Technical Stack

- **Java 11+**
- **Servlet 4.0 / JSP 2.3**
- **SQLite** (via sqlite-jdbc) - No external DB server required
- **jBCrypt** for password hashing
- **Bootstrap 5** (CDN) for UI
- **JSTL** for JSP tag library
- **Maven** WAR project (deployable to Tomcat 9)

## Getting Started

### Prerequisites

- Java 11 or higher
- Apache Maven 3.6+
- Apache Tomcat 9

### Build

```bash
cd loose-notes
mvn clean package
```

### Deploy

Copy the generated `target/loose-notes.war` to your Tomcat `webapps/` directory.

### Run with embedded Tomcat (Maven plugin)

```bash
mvn tomcat7:run
```

The application will be available at: http://localhost:8080/loose-notes

### Default Admin Account

On first startup, a default admin account is created:
- **Username**: `admin`
- **Password**: `admin123`

**Change the admin password immediately after first login!**

## Database

The SQLite database is automatically created on first startup at:
```
~/.loosenotes/loosenotes.db
```

File uploads are stored at:
```
~/.loosenotes/uploads/
```

## Application Structure

```
src/
  main/
    java/com/loosenotes/
      dao/           # Data Access Objects
      filter/        # AuthFilter
      model/         # POJO models
      servlet/       # HTTP Servlets
      util/          # DBUtil, PasswordUtil, FileUtil
    resources/db/
      schema.sql     # Reference SQL schema
    webapp/
      WEB-INF/
        jsp/         # JSP view templates
        web.xml      # Servlet configuration
      css/           # Custom CSS
      index.jsp      # Root redirect
```

## URL Routes

| URL | Description |
|-----|-------------|
| `/` | Redirects to dashboard or login |
| `/login` | Login page |
| `/register` | Registration page |
| `/logout` | Log out |
| `/dashboard` | User's notes list |
| `/notes?action=create` | Create new note |
| `/notes?action=view&id=X` | View note |
| `/notes?action=edit&id=X` | Edit note |
| `/search?q=term` | Search notes |
| `/top-rated` | Top rated public notes |
| `/profile` | User profile |
| `/share?token=X` | View shared note (no auth) |
| `/password-reset` | Password reset request |
| `/admin` | Admin dashboard |
| `/admin?action=users` | Manage users |

## Password Reset (Demo Mode)

Since this is a demo application, no emails are sent. When you request a password reset, the token/link is displayed directly on screen. In a production environment, this would be sent via email.

## Notes

- SQLite supports concurrent reads but single-threaded writes. For high-concurrency scenarios, consider migrating to PostgreSQL or MySQL.
- File uploads are limited to 10MB per file.
- Sessions expire after 60 minutes of inactivity.
