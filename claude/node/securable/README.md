# Loose Notes

A secure, multi-user note-taking web application built with **Node.js / Express.js**, **SQLite**, and **EJS** templates.

---

## Features

| Requirement | Description |
|---|---|
| REQ-001 | User registration with username, email, and password |
| REQ-002 | Cookie-based authentication with session management |
| REQ-003 | Password reset via time-limited token (1 hour) |
| REQ-004 | Create notes with title, content, and private-by-default visibility |
| REQ-005 | File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG; max 10 MB) |
| REQ-006 | Edit note title, content, and public/private toggle |
| REQ-007 | Delete notes with all attachments, ratings, and share links |
| REQ-008 | Generate / revoke unique UUID share links (no auth required to view) |
| REQ-009 | Public / private note visibility toggle |
| REQ-010 | 1–5 star ratings with optional comments; edit own rating |
| REQ-011 | Note owner sees all ratings with averages |
| REQ-012 | Full-text search across owned + public notes |
| REQ-013 | Admin dashboard with user list, note count, and activity log |
| REQ-014 | User profile: update username, email, and password |
| REQ-015 | Top-rated public notes (min 3 ratings required) |
| REQ-016 | Admin can reassign note ownership |

---

## Setup

### Prerequisites

- Node.js 18+
- npm 9+
- Windows: [Visual Studio Build Tools](https://visualstudio.microsoft.com/visual-cpp-build-tools/) (needed by `better-sqlite3`)

### Install

```bash
npm install
```

### Configure

```bash
cp .env.example .env
# Edit .env and set SESSION_SECRET and optionally SMTP_* variables
```

> **Development tip:** If SMTP is not configured, password reset links are printed to the console log — no email server needed.

### Run

```bash
# Development (auto-restart on changes)
npm run dev

# Production
npm start
```

The app listens on `http://localhost:3000` by default (set `PORT` in `.env` to change).

### First Admin Account

Register a user normally, then promote them to admin directly in the SQLite database:

```bash
# Install sqlite3 CLI or use any SQLite browser
sqlite3 data/loose-notes.db "UPDATE users SET role='admin' WHERE username='your_username';"
```

---

## Project Structure

```
src/
  app.js              Express application entry point
  config/
    db.js             SQLite schema initialisation
    logger.js         Winston structured logger
    mailer.js         Nodemailer (password reset emails)
  middleware/
    auth.js           requireAuth, requireAdmin, loadUser
    csrf.js           Synchronised Token Pattern CSRF protection
    upload.js         Multer file upload with allowlist validation
  routes/
    auth.js           Register, login, logout, password reset
    notes.js          CRUD, attachments, ratings, share link management
    admin.js          Admin dashboard, user list, note reassignment
    profile.js        Profile info and password change
    search.js         Keyword search (title + content)
    share.js          Public share-link viewer
  views/
    partials/         EJS partials (head, nav, footer, flash, errors)
    auth/             Login, register, forgot/reset password views
    notes/            List, create, edit, view, share views
    admin/            Dashboard and users views
    profile/          Profile edit view
    *.ejs             Search, top-rated, share-view, error pages
public/
  css/style.css       Custom styles (Bootstrap 5 used via CDN)
uploads/              Stored attachment files (UUID-named)
data/                 SQLite database files (auto-created)
logs/                 Application log files (auto-created)
```

---

## SSEM Security Score Summary

| Attribute | Implementation |
|---|---|
| **Secure Defaults** | Notes default to private; sessions default to HttpOnly, SameSite=lax |
| **Session Management** | express-session with SQLite store; session regenerated on login (fixation prevention); 24-hour cookie lifetime |
| **Encryption / Hashing** | bcryptjs (PBKDF2-equivalent, cost=12) for password storage; UUID v4 for share tokens and stored filenames |
| **Authentication** | Server-side session; generic error messages on failed login (no username/email enumeration) |
| **Authorisation** | Ownership checks before every edit/delete; role-based admin guard; private note access control on every read |
| **CSRF Protection** | Custom Synchronised Token Pattern middleware using `crypto.timingSafeEqual`; token in every state-changing form |
| **Input Validation** | `express-validator` on all POST routes; parameterised queries (better-sqlite3 prepared statements — zero SQL injection risk) |
| **Output Encoding** | EJS auto-escapes all `<%= %>` interpolations (XSS prevention); raw HTML only used for newline conversion on note body |
| **File Upload Security** | Extension + MIME-type allowlist; UUID-named files to prevent collisions and directory traversal; `path.basename()` before every file access |
| **Security Headers** | `helmet` with strict Content Security Policy (self + Bootstrap CDN only); no inline scripts |
| **Audit Logging** | Winston logs auth events, admin actions, and errors with timestamps; activity_log DB table for dashboard display |
| **Information Disclosure** | Password reset response is identical regardless of whether email exists (no enumeration); production mode suppresses stack traces |
| **Dependency Surface** | Minimal production dependencies; no deprecated or high-severity packages |
