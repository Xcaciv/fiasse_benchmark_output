# Loose Notes

Loose Notes is a JSP/Servlet note-sharing application that implements the supplied PRD in a Java web stack. It supports multi-user accounts, private/public notes, attachments, share links, ratings, password reset, profile updates, search, top-rated notes, and an admin dashboard with ownership reassignment and activity logs.

## Stack

- Java 11
- JSP + Servlets (Servlet 4.0)
- SQLite for relational persistence
- BCrypt password hashing
- Bootstrap 5 for responsive UI

## Features

- User registration, login, logout, and one-hour password reset flow
- Note create/edit/delete with public/private visibility
- File uploads for `pdf`, `doc`, `docx`, `txt`, `png`, `jpg`, `jpeg`
- Share links for any note, with regeneration and revocation
- Keyword search across visible notes
- Ratings with 1-5 stars and optional comments
- Top-rated public notes page (minimum 3 ratings)
- Profile management for username, email, and password
- Admin dashboard with user search, note reassignment, and recent activity logs

## Project layout

- `pom.xml` - Maven WAR build
- `src/main/java` - application code
- `src/main/webapp` - JSP views, static assets, and `WEB-INF/web.xml`

## Prerequisites

- Java 11+
- Maven 3.9+ (or any recent Maven able to build a WAR)
- Tomcat 9+ or another servlet container compatible with `javax.servlet` 4.0

## Local storage

The app stores runtime data outside the WAR under a home directory.

- Default home: `%USERPROFILE%\.loosenotes`
- Override with environment variable `LOOSENOTES_HOME`
- Demo password-reset link behavior can be disabled with `LOOSENOTES_DEMO_MODE=false`
- The app creates:
  - `data\loosenotes.db`
  - `uploads\...`

## Build

```powershell
mvn clean package
```

This creates `target\loose-notes.war`.

## Run on Tomcat

1. Build the WAR.
2. Copy `target\loose-notes.war` into Tomcat's `webapps` folder.
3. Start Tomcat.
4. Open `http://localhost:8080/loose-notes/`.

If you want a different runtime storage directory:

```powershell
$env:LOOSENOTES_HOME = 'C:\temp\loosenotes'
$env:LOOSENOTES_DEMO_MODE = 'false'
```

Set that before starting Tomcat so the webapp can read it during bootstrap.

## Demo credentials

The app seeds a default admin account on first startup:

- Username: `admin`
- Email: `admin@local.test`
- Password: `Admin123!`

Change that password after signing in.

## Password reset behavior

SMTP is not configured in this self-contained demo. Instead, the forgot-password page shows the generated reset link directly when demo mode is enabled so the full workflow still works locally without external infrastructure. Set `LOOSENOTES_DEMO_MODE=false` to turn that off.

## Notes about validation and security

- Passwords are hashed with BCrypt.
- SQLite access uses prepared statements.
- State-changing forms include CSRF tokens.
- Ownership checks are enforced for edit/delete/share operations.
- Share tokens are not written to the activity log.

## What I adapted from the PRD

The PRD referenced ASP.NET Core MVC and Identity, but this implementation translates the same requirements into a Java JSP/Servlet architecture with SQLite and manual session-based authentication.
