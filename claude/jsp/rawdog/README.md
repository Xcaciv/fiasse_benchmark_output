# Loose Notes

A multi-user note-taking web application built with Java Servlets, JSP, and SQLite.

## Features

- User registration and authentication
- Create, edit, delete notes
- Public/private note visibility
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Share notes via unique link tokens
- Rate notes (1-5 stars with optional comment)
- Search notes (title/content)
- Top rated notes list
- Password reset via token
- Admin dashboard with user management
- Note ownership reassignment (admin)

## Tech Stack

- Java 17
- Jakarta Servlet 6.0 / JSP 3.1
- JSTL 3.0
- SQLite (via sqlite-jdbc)
- jBCrypt for password hashing
- Apache Commons FileUpload
- Bootstrap 5
- Apache Tomcat 10

## Building and Running

### Prerequisites
- Java 17+
- Maven 3.6+

### Run with embedded Tomcat

```bash
mvn tomcat10:run
```

Then open http://localhost:8080/

### Build WAR

```bash
mvn clean package
```

Deploy `target/loose-notes.war` to your Tomcat 10 instance.

## Configuration

The SQLite database is stored at `${user.home}/loosenotes-data/loosenotes.db` by default.
File uploads are stored in `${user.home}/loosenotes-data/uploads/`.

## Default Admin Account

On first run, seed data creates an admin user:
- Username: `admin`
- Password: `admin123`

Change this password immediately after first login.
