# Loose Notes

A securable Node.js/Express web application for personal and shared note-taking, built with FIASSE/SSEM engineering principles.

## Prerequisites

- Node.js >= 18.0.0
- npm >= 9.x

## Setup

1. **Install dependencies**
   ```bash
   npm install
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env and set a strong SESSION_SECRET (32+ random characters)
   ```

3. **Start the server**
   ```bash
   npm start
   # Development with auto-reload:
   npm run dev
   ```

4. **Access the app**
   Open http://localhost:3000 in your browser.

## Features

- User registration and authentication with bcrypt password hashing
- Create, edit, delete notes with public/private visibility
- File attachments with magic-byte validation
- Note ratings (1-5 stars) with comments
- Shareable links via hashed tokens
- Full-text note search
- Admin dashboard with audit log
- Password reset flow (email stub)

## SSEM Attribute Coverage

### Maintainability

| Attribute | Implementation |
|-----------|---------------|
| **Analyzability** | All functions ≤ 30 LoC. Descriptive naming throughout. Clear module boundaries (models, services, controllers, routes). |
| **Modifiability** | Centralized config in `src/config/index.js`. Security logic (auth, CSRF, rate-limit) in dedicated middleware. Dependency injection via `db` parameter. |
| **Testability** | `db` injected into all controllers and models. Services use pure functions. Middleware isolated and independently testable. |

### Trustworthiness

| Attribute | Implementation |
|-----------|---------------|
| **Confidentiality** | No secrets in code or logs. `SESSION_SECRET` from env. Passwords hashed with bcrypt (12 rounds). Token values stored as SHA-256 hashes. Least-privilege data access in all SQL queries. |
| **Accountability** | Structured audit log (append-only) for all security-sensitive events: login, logout, note CRUD, attachments, role changes, re-auth. Winston structured logging with timestamps. |
| **Authenticity** | Session signed with `SESSION_SECRET`. Session regenerated on login (fixation prevention). CSRF double-submit pattern for all state-changing requests. Re-auth gate for destructive admin actions. |

### Reliability

| Attribute | Implementation |
|-----------|---------------|
| **Availability** | Express-rate-limit on login (5/15min), registration, search, and general routes. 30-second server timeout. Resource limits on body parser (1MB) and file uploads (10MB). |
| **Integrity** | Input validated at every trust boundary (canonicalize → sanitize → validate). Parameterized queries only (better-sqlite3 prepared statements). Visibility enforced in SQL WHERE clause. Owner derived from session, never request body. |
| **Resilience** | Specific error handling in each controller. File deletion failures logged but do not crash requests. Graceful SIGTERM shutdown. Foreign key constraints with cascade deletes. WAL mode for SQLite durability. |

## Security Notes

- Set `NODE_ENV=production` and provide a strong `SESSION_SECRET` in production
- HTTPS is required in production (the `secure` cookie flag is enabled automatically)
- The email service is a stub — integrate a real email provider for password resets
- Review upload MIME type allowlist before deploying
