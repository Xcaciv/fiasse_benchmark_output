# Loose Notes Web Application

Loose Notes is a secure-by-default Express.js web application for creating, sharing, rating, and administering notes with SQLite-backed persistence.

## Features

- User registration, login, logout, and password reset with time-limited reset tokens.
- Private-by-default notes with public visibility controls.
- Note creation, editing, deletion, attachment uploads, and share links.
- Rating and comment support with top-rated public note listings.
- Profile management for username, email, and password changes.
- Admin dashboard with user search, activity logs, and note ownership reassignment.

## Stack

- Node.js with Express.js and EJS views
- SQLite via `better-sqlite3`
- Cookie sessions via `express-session` and a custom SQLite-backed session store
- CSRF protection, Helmet headers, secure password hashing, and input validation

## Setup

1. Install dependencies:

   ```bash
   npm install
   ```

2. Optionally create an admin account:

   ```bash
   npm run seed:admin -- --username admin --email admin@example.com --password "AdminPassw0rd!"
   ```

3. Start the application:

   ```bash
   npm start
   ```

4. Open `http://localhost:3000`.

## Local data locations

- Database: `data/loose-notes.sqlite`
- Uploaded files: `data/uploads/`
- Generated password reset emails: `data/mail/`
- Application log: `data/logs/application.log`

## Password reset flow

Because this generated project does not assume external SMTP access, password reset emails are written to `data/mail/` as `.eml` files. Open the newest file and use the included link.

## Validation

Run the included test suite:

```bash
npm test
```

## SSEM attribute coverage summary

The codebase addresses nine core secure-engineering attributes as follows:

1. **Authentication**: Users authenticate with username and password, sessions are cookie-backed, and password reset tokens are time-limited and single-use.
2. **Authorization**: Route-level and ownership checks restrict editing, deletion, administration, and attachment access.
3. **Confidentiality**: Notes default to private, share links are bearer secrets, session cookies are `httpOnly`, and sensitive secrets are excluded from logs.
4. **Integrity**: SQLite foreign keys, prepared statements, server-side validation, CSRF tokens, and token hashing protect data correctness and state-changing operations.
5. **Availability**: The app uses durable SQLite storage, file persistence, graceful error handling, and a health endpoint for basic service monitoring.
6. **Accountability**: Authentication events, admin actions, and note lifecycle changes are written to an activity log and file-based application logs.
7. **Privacy**: Passwords are PBKDF2-hashed, reset tokens are hashed before storage, and mail output avoids exposing credentials in logs.
8. **Resilience**: Centralized error handling, attachment type/size validation, and transactional note mutations reduce failure blast radius.
9. **Maintainability**: The project uses a clear Express MVC-style structure, focused helpers, database initialization, and a small automated smoke test suite.
