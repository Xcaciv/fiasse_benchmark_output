# Loose Notes

A production-quality, multi-user note-taking platform built with Node.js/Express.js and engineered to FIASSE/SSEM securable standards.

## Features

- User registration, login, logout, password reset (time-limited tokens)
- Create, edit, delete notes (public/private visibility)
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG/JPEG; max 10 MB, 5 per note)
- Note sharing via unique, revocable share links (no auth required to view)
- Note ratings (1–5 stars + optional comment; no self-rating)
- Full-text search across owned and public notes
- Top-rated public notes leaderboard (min 3 ratings)
- Admin dashboard (user count, note count, activity log, user management, note ownership reassignment)
- User profile management (update username, email, password)

## Setup

```bash
git clone <repository-url>
cd loose-notes
npm install
cp .env.example .env
# Edit .env and fill in SESSION_SECRET, SMTP settings, etc.
npm start
```

## Development

```bash
npm run dev   # Starts with nodemon (auto-restart on changes)
```

## Running Tests

```bash
npm test
```

## Environment Variables

| Variable | Description | Default |
|---|---|---|
| `NODE_ENV` | `development` / `production` / `test` | `development` |
| `PORT` | HTTP port | `3000` |
| `SESSION_SECRET` | Secret for signing session cookies (min 32 chars in production) | — |
| `DATABASE_URL` | PostgreSQL connection string or SQLite file path | `./data/database.sqlite` |
| `UPLOAD_DIR` | Directory for uploaded files | `./uploads` |
| `LOG_LEVEL` | Winston log level | `info` |
| `LOG_FILE` | Path for JSON log file | `./logs/app.log` |
| `SMTP_HOST` | SMTP server hostname | — |
| `SMTP_PORT` | SMTP server port | `587` |
| `SMTP_USER` | SMTP username | — |
| `SMTP_PASS` | SMTP password | — |
| `FROM_EMAIL` | From address for outgoing email | `noreply@example.com` |
| `BASE_URL` | Public base URL (used in email links) | `http://localhost:3000` |

## SSEM Attribute Coverage

| Attribute | Pillar | How It Is Addressed |
|---|---|---|
| **Analyzability** | Maintainability | Functions ≤ 30 LoC, cyclomatic complexity < 10, descriptive naming, single-purpose modules |
| **Modifiability** | Maintainability | Dependency injection (passport factory, upload config), centralized auth/crypto/validation modules, all config via env vars |
| **Testability** | Maintainability | Security controls isolated in dedicated modules (validators, tokenUtils, auditService); Jest + Supertest test suite |
| **Confidentiality** | Trustworthiness | Passwords hashed with bcrypt (cost 12); reset tokens stored as SHA-256 hashes; no secrets in logs/responses; `passwordHash` excluded by default scope |
| **Accountability** | Trustworthiness | Structured Winston JSON logging; ActivityLog DB table; all auth events, authz denials, admin actions, file operations logged with userId/action/IP |
| **Authenticity** | Trustworthiness | Passport.js local strategy; session regeneration on login (prevents fixation); secure, httpOnly, sameSite:strict cookies; CSRF protection via csurf |
| **Availability** | Reliability | Rate limiting on auth endpoints (5 req/15 min); file size (10 MB) and file count (5) limits; session max age; graceful error handling |
| **Integrity** | Reliability | Input validated at all trust boundaries (canonicalize → sanitize → validate); Derived Integrity Principle (userId from session, not request body); parameterized queries via Sequelize ORM; path traversal prevention on file downloads |
| **Resilience** | Reliability | Specific exception handling (no bare catch-all); AppError class distinguishes operational vs programmer bugs; email service timeout (10s); graceful handling of missing files; CSRF error handling |

## Security Notes

- **CSRF**: This project uses the `csurf` package (deprecated but compatible with Express 4). For Express 5 migration, replace with a maintained alternative such as `csrf-csrf`.
- **Password Reset Tokens**: Raw tokens are sent only in email links; only SHA-256 hashes are stored in the database.
- **Session Fixation**: Sessions are regenerated via `req.session.regenerate()` on successful login.
- **Path Traversal**: File download paths are resolved with `path.resolve()` and verified to be within `UPLOAD_DIR`.
- **HTTP Method Override**: PUT/DELETE are emulated via `_method` hidden inputs for HTML form compatibility.
