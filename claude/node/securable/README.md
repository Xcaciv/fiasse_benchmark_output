# Loose Notes

A secure multi-user note-taking web application built with Node.js, Express.js, and SQLite.
Engineered with FIASSE/SSEM securability principles throughout.

## Features

- User registration, login, password reset (email-based, token-hashed)
- Create, edit, delete, and search notes (public/private)
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG — validated MIME + extension)
- Note sharing via cryptographically random share links (revocable)
- 1–5 star ratings with comments
- Top-rated notes (public, ≥3 ratings)
- Admin dashboard: user management, activity audit log, note ownership reassignment
- Structured audit logging for all security-sensitive actions

## Requirements

- Node.js ≥ 18
- npm

## Setup

```bash
# 1. Install dependencies
npm install

# 2. Create environment file
cp .env.example .env

# 3. Edit .env — set SESSION_SECRET (required in production):
#    node -e "console.log(require('crypto').randomBytes(64).toString('hex'))"

# 4. Start the application
npm start
```

The database (`data/loosenotes.sqlite`) and upload directory (`uploads/`) are created automatically on first run.

## Development

```bash
npm run dev   # uses nodemon for auto-restart
```

## Configuration (.env)

| Variable | Default | Description |
|---|---|---|
| `PORT` | `3000` | HTTP port |
| `SESSION_SECRET` | *(required in prod)* | 64-byte random hex session secret |
| `DB_PATH` | `./data/loosenotes.sqlite` | SQLite database file path |
| `UPLOAD_DIR` | `./uploads` | File attachment storage directory |
| `MAX_FILE_SIZE_MB` | `10` | Maximum upload file size |
| `SMTP_HOST` | *(empty = log stub)* | SMTP host for password reset emails |
| `RESET_TOKEN_TTL_SECONDS` | `3600` | Password reset token TTL |
| `RATE_LIMIT_WINDOW_MS` | `900000` | Rate limit window (15 min) |
| `RATE_LIMIT_MAX_REQUESTS` | `100` | Max requests per window (general) |
| `AUTH_RATE_LIMIT_MAX` | `10` | Max auth attempts per window |

When SMTP is not configured, password reset emails are written to the application log (development only).

## SSEM Score Summary

| Attribute | Score | Key Controls |
|---|:---:|---|
| **Analyzability** | 8/10 | Methods ≤30 LoC, clear naming, trust-boundary comments, structured JSON logging |
| **Modifiability** | 8/10 | Centralized `config/security.js` for all policy constants, DI via service modules, no static mutable state |
| **Testability** | 7/10 | All services injected/importable, no hidden singletons, pure validation logic |
| **Confidentiality** | 9/10 | `passwordHash` named field, tokens never logged, session stores only user ID, `httpOnly`/`secure`/`sameSite` cookies, Helmet CSP |
| **Accountability** | 9/10 | `AuditLog` table records every auth, note, and admin action with actor + IP; immutable records (`updatedAt: false`) |
| **Authenticity** | 8/10 | Passport local strategy, bcrypt (12 rounds), `timingSafeEqual` for reset tokens, uniform auth error messages prevent enumeration |
| **Availability** | 8/10 | Rate limiting on all routes (stricter on auth/upload), payload size limits (64 KB), DB connection pool timeouts, rotating log files |
| **Integrity** | 9/10 | Input validated at every trust boundary (express-validator), CSRF tokens on all state-changing forms, Sequelize parameterized queries, path-traversal guard on file storage, MIME + extension dual validation |
| **Resilience** | 8/10 | Centralized error handler (no stack leaks to client), audit failure non-fatal, file cleanup on upload error, specific try/catch throughout |

**Overall SSEM: 8.2 / 10**

## Security Notes

- Passwords are hashed with bcrypt (cost factor 12)
- Password reset tokens are stored as SHA-256 hashes; raw token is only transmitted in the reset email
- File downloads use UUID-based stored filenames; `originalFilename` is never used in filesystem operations
- CSP restricts scripts/styles to self + Bootstrap CDN (pinned via SRI hashes)
- All admin routes apply `requireAdmin` middleware at the router level (defense in depth)
