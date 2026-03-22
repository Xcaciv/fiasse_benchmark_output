# Loose Notes JSP/Servlet Application

Loose Notes is a multi-user note-taking web application built with JSP, servlets, SQLite, and local file storage. It implements the PRD features for registration, authentication, password reset, note CRUD, attachments, sharing, ratings, search, profile management, and admin controls.

## Project structure

- `src/main/java/com/loosenotes/` contains the servlet controllers, DAO layer, security helpers, storage services, and bootstrap logic.
- `src/main/webapp/WEB-INF/jsp/` contains the JSP views.
- `data/storage/` stores uploaded attachments.
- `data/outbox/` stores password reset emails for local/demo use.

## Setup and run

1. Install Java 11+ and Maven 3.9+.
2. From the project root, build the WAR:
   - `mvn clean package`
3. Deploy `target/loose-notes.war` to a Servlet 4 compatible container such as Tomcat 9.
4. Optionally run directly with Jetty during development:
   - `mvn jetty:run`
5. Open `http://localhost:8080/loose-notes` unless your container uses a different context path.

Runtime data defaults to `./data`. You can override that path with the system property `-Dloosenotes.dataDir=C:\\path\\to\\runtime-data`.

## Default admin account

On first startup, the application seeds an admin account if none exists:

- Username: `admin`
- Email: `admin@example.com`
- Password: `ChangeMe123!`

Change this password immediately after the first login.

## Password reset email behavior

For safety and portability, the demo app writes password reset emails to `data/outbox/` instead of sending real mail. Each file contains the one-time reset link and expires after one hour once generated.

## Attachment handling

Supported file extensions are `pdf`, `doc`, `docx`, `txt`, `png`, `jpg`, and `jpeg`.

Each uploaded attachment is stored with a generated unique filename, while the original filename, media type, and size are preserved in SQLite metadata.

## SSEM attribute coverage summary

The generated code addresses nine security-oriented attributes as follows:

1. **Authentication**: Registration, login, logout, session fixation protection, and password reset token workflows are implemented in `AuthServlet`, `UserDao`, and `PasswordResetDao`.
2. **Authorization**: Ownership and admin checks gate note editing, deletion, reassignment, and protected profile/admin routes in `BaseServlet`, `NotesServlet`, and `AdminServlet`.
3. **Confidentiality**: Private notes remain owner-only, share links use random high-entropy tokens stored as SHA-256 hashes, and security headers reduce passive disclosure.
4. **Integrity**: CSRF tokens protect state-changing forms, prepared statements prevent SQL injection, and attachment path normalization prevents traversal.
5. **Availability**: SQLite busy timeout, bounded upload sizes, and simple local storage reduce avoidable runtime contention and resource exhaustion.
6. **Accountability**: Authentication events, admin actions, and note lifecycle changes are recorded in `activity_logs` and surfaced on the admin dashboard.
7. **Input Validation**: Server-side validation enforces username, email, password, note, rating, and file constraints before persistence.
8. **Privacy / Minimization**: The app avoids logging passwords or raw reset/share tokens and keeps password reset delivery in a local outbox rather than broadcast email during demo use.
9. **Operational Hardening**: CSP, `X-Frame-Options`, `nosniff`, `HttpOnly` session cookies, no-cache headers for dynamic pages, and fail-closed error handling are applied centrally in the filter and bootstrap code.

## Notes for deployment

- Put the app behind HTTPS in production.
- Rotate the seeded admin password immediately.
- Back up `data/loose-notes.db` and `data/storage/` together if you need to preserve note attachments consistently.
