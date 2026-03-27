# Loose Notes

A note-taking web application built with Node.js/Express.js, engineered to embody the nine SSEM (Securable Software Engineering Model) attributes across three pillars: Maintainability, Trustworthiness, and Reliability.

## Setup Instructions

### Prerequisites
- Node.js >= 18.0.0
- npm

### Installation

```bash
# 1. Clone or navigate to the project directory
cd loose-notes

# 2. Install dependencies
npm install

# 3. Configure environment
cp .env.example .env
# Edit .env and set a strong SESSION_SECRET (32+ characters)

# 4. Start the application
npm start
```

The server starts on `http://localhost:3000` by default.

### Environment Variables

| Variable | Description | Default |
|---|---|---|
| `PORT` | HTTP port | `3000` |
| `SESSION_SECRET` | Session signing secret (required, 32+ chars) | none |
| `DATABASE_PATH` | SQLite database file path | `./data/loose-notes.db` |
| `UPLOAD_PATH` | File upload directory | `./uploads` |
| `MAX_FILE_SIZE_MB` | Maximum attachment size in MB | `10` |
| `ADMIN_PASSWORD` | Default admin password (change immediately) | `AdminP@ss1234567890!` |
| `LOG_LEVEL` | Winston log level | `info` |
| `TRUST_PROXY` | Set `true` if behind a reverse proxy | `false` |
| `NODE_ENV` | Environment (`development`/`production`) | `development` |

### Default Admin Credentials

**IMPORTANT:** Change the admin password immediately after first login.

- **Email:** `admin@loosenotes.local`
- **Password:** Value of `ADMIN_PASSWORD` env var (default: `AdminP@ss1234567890!`)

---

## Architecture Overview

```
loose-notes/
├── app.js            # Express app factory (middleware, routes, CSRF, sessions)
├── server.js         # HTTP server startup, graceful shutdown
├── config/           # Database, logger, and application constants
├── middleware/        # Auth, authorization, rate limiting, error handling, security headers
├── models/           # Sequelize ORM models with associations
├── services/         # Business logic layer (auth, notes, files, ratings, sharing, admin, audit)
├── routes/           # Express route handlers (thin controllers calling services)
├── views/            # EJS templates with base layout
└── public/           # Static assets (CSS)
```

All data is stored in a SQLite database. File attachments are stored in the `uploads/` directory with UUID-based filenames. Audit logs are append-only records in the database.

---

## SSEM Attribute Coverage

### Maintainability

**Analyzability**: All functions are constrained to ≤ 30 lines of code with cyclomatic complexity below 10. Every module has a clear single responsibility: services handle business logic, routes handle HTTP concerns, middleware handles cross-cutting concerns. Trust boundaries are annotated with inline comments indicating where external data enters the system. Structured logging with dot-notated event names (e.g. `auth.login`, `note.created`) makes log streams parseable and auditable.

**Modifiability**: Security logic is centralized in dedicated modules: `middleware/authenticate.js`, `middleware/authorize.js`, `middleware/security.js`, and `config/constants.js`. All tuneable limits (password length, session timeout, lockout threshold, rate limits) are declared in a single `constants.js` file. Services are injected as modules into routes, not instantiated inline, making swapping implementations straightforward. The email service is a stub interface that logs instead of sending — replacing it with SMTP requires changing only `services/emailService.js`.

**Testability**: Services expose pure functions that accept dependencies as arguments, making them mockable without framework modifications. No global state is mutated outside of request-scoped session objects. Route handlers delegate to service calls that can be tested independently.

### Trustworthiness

**Confidentiality**: Passwords are stored as bcrypt hashes (cost factor 12) and never appear in logs or responses. Password reset tokens are stored as SHA-256 hashes; raw tokens appear only in reset URLs. File uploads are stored with UUID-based filenames outside the `public/` directory, preventing direct URL access. Email addresses are masked in logs. The User model's `defaultScope` excludes `passwordHash`, `passwordResetHash`, and `passwordResetExpiry` from all ordinary queries. Cache-Control headers are set to `no-store` for all authenticated responses.

**Accountability**: Every security-sensitive action is written to the `audit_logs` table via `auditService.log()`. The audit log is append-only — no `DELETE` or `UPDATE` operations exist anywhere in the codebase. Each log entry records: event name, actor ID, target ID, outcome (success/failure/denied), client IP, and a correlation ID for request tracing. Session login/logout, registration, password changes, note creation/deletion, file uploads, and share link operations are all logged.

**Authenticity**: Session IDs are regenerated on login to prevent session fixation attacks. The session store uses `connect-sqlite3` rather than in-memory storage, preventing session loss on restart. CSRF tokens are required on all mutating forms. The `authenticate` middleware re-queries the database on every request rather than trusting cached session data alone, ensuring deactivated accounts are immediately locked out. Admin role is re-verified from the database in `requireAdmin`, not from cached session state.

### Reliability

**Availability**: Six distinct rate limiters protect against DoS and brute force: registration (10/hour/IP), login (20/15min/IP), search (30/min/session), top-rated (120/min/IP), share view (60/min/IP), and a general (100/min/IP) limiter applied application-wide. Server timeout is set to 30 seconds. Account lockout after 5 failed login attempts for 30 minutes. Graceful shutdown handles `SIGTERM`/`SIGINT` with a 10-second force-exit fallback.

**Integrity**: Input validation using `express-validator` is applied at every trust boundary (all routes with user input). The note service always re-queries ownership from the database on edit and delete operations, rejecting any client-supplied ownership claims. Visibility is validated against an explicit allowlist (`'public'` or default to `'private'`). File attachments are validated by both extension allowlist AND magic-byte MIME detection via the `file-type` library. Sequelize ORM with parameterized queries is used exclusively — no raw SQL string interpolation exists in the codebase. Rating values are validated as integers in range [1, 5] at both model and service layers.

**Resilience**: The `auditService.log()` function wraps all database writes in try/catch and never throws, so audit failures cannot crash request processing. File deletion failures during cascade delete are logged and swallowed to allow the transaction to complete. The global `errorHandler` catches all unhandled errors, logs the full stack trace server-side, and returns only a correlation ID to the client. Unhandled promise rejections are caught via `process.on('unhandledRejection')` and logged without crashing. The `notFoundHandler` handles all 404 cases before the error handler.
