# Loose Notes

A Java web application for note-taking built with JSP and Servlets, backed by SQLite.

## Features

- User registration and authentication
- Create, edit, delete notes with PRIVATE/PUBLIC visibility
- File attachments (PDF, Word, TXT, images up to 10 MB)
- Share notes via token-based links (no login required for recipient)
- Note ratings (1–5 stars) with comments
- Full-text search across owned and public notes
- Top-rated public notes leaderboard
- Password reset (dev-mode: link shown on screen)
- Admin dashboard: user/note stats and note ownership reassignment

## Requirements

- Java 11+
- Apache Maven 3.6+
- Apache Tomcat 9.x

## Build

```bash
mvn clean package
```

The WAR is produced at `target/loose-notes-1.0-SNAPSHOT.war`.

## Deploy

Copy the WAR to Tomcat's `webapps/` directory and start Tomcat:

```bash
cp target/loose-notes-1.0-SNAPSHOT.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```

Navigate to: `http://localhost:8080/loose-notes-1.0-SNAPSHOT/`

## Data Storage

The application stores data under `~/loose-notes-data/`:

| Path | Contents |
|------|----------|
| `~/loose-notes-data/loosenotes.db` | SQLite database |
| `~/loose-notes-data/uploads/` | Uploaded attachment files |

Both directories are created automatically on first startup.

## Default Admin Credentials

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin123` |

> **Important:** Change the admin password immediately after first login via the Profile page.

## Tech Stack

- Java 11
- JSP 2.3 / Servlet 4.0
- SQLite via sqlite-jdbc 3.43.0.0
- Password hashing via jBCrypt 0.4
- Bootstrap 5.3 (CDN)
- Apache Tomcat 9
