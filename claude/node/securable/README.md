# Loose Notes

A secure multi-user note-taking web application built with Node.js and Express.js.

## Features

- User registration, login, and password reset via email
- Create, edit, delete, and search notes
- Public/private visibility per note
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG — max 5 MB)
- Note rating (1–5 stars) with optional comments
- Shareable links (unauthenticated view)
- Top-rated public notes page
- Admin dashboard — user management, note reassignment, audit log
- Full audit trail for security-sensitive actions

---

## Setup

### Prerequisites

- Node.js ≥ 18
- npm ≥ 9

### Install dependencies

```bash
npm install
```

### Configure environment

```bash
cp .env.example .env
```

Edit `.env` and set at minimum:

| Variable | Description |
|---|---|
| `SESSION_SECRET` | 64-byte random hex. Generate: `node -e "require('crypto').randomBytes(64).toString('hex')"` |
| `ADMIN_EMAIL` | Email address that gets admin role on first registration |
| `SMTP_*` | SMTP credentials for password-reset emails (optional in dev — emails are logged to console) |

### Run in development

```bash
npm run dev
```

The server starts on `http://localhost:3000`. The SQLite database is created automatically at `./data/loosenotes.sqlite`.

### Run in production

```bash
NODE_ENV=production npm start
```

Set `SESSION_SECRET` to a strong random value and all required env vars before starting.

---

## Running tests

```bash
npm test
```

---

## Project structure

```
src/
├── app.js            Express application factory (middleware, routes)
├── server.js         Entry point — DB sync, HTTP server, graceful shutdown
├── config/
│   ├── database.js   Sequelize/SQLite connection
│   ├── logger.js     Winston structured logger
│   ├── passport.js   Passport.js local strategy
│   └── security.js   Security constants from environment
├── middleware/
│   ├── auth.js       requireAuth / requireAdmin / redirectIfAuthenticated
│   ├── errorHandler.js  Centralised error handler + 404 handler
│   ├── rateLimiter.js   express-rate-limit (login + general)
│   └── validate.js   express-validator result handler
├── models/           Sequelize model definitions
├── routes/           Express routers (auth, notes, attachments, profile, share, admin)
├── services/         Business logic (authService, noteService, fileService, …)
└── views/            EJS templates
public/
└── css/style.css     Responsive stylesheet (no external CDN dependencies)
uploads/              Uploaded files (excluded from git)
data/                 SQLite database (excluded from git, auto-created)
```

---

## SSEM Attribute Coverage

This application was generated under the **FIASSE Securability Engineering** workflow with all nine SSEM attributes applied as engineering constraints.

### Maintainability

| Attribute | Implementation |
|---|---|
| **Analyzability** | All service functions are ≤ 30 LoC with a single responsibility. Trust boundary entry points (routes) are clearly separated from business logic (services). Meaningful naming throughout (`assertOwnership`, `clampRating`, `canonicalEmail`). No dead code paths. |
| **Modifiability** | Security logic is centralised: authentication in `config/passport.js`, authorisation guards in `middleware/auth.js`, rate limiting in `middleware/rateLimiter.js`, CSRF in `app.js`, input validation rules inline with each route for traceability. Configuration is externalised entirely to environment variables via `config/security.js`. Changing the database engine requires only `config/database.js`. |
| **Testability** | Services are pure async functions with injected Sequelize models — they can be tested with mock DB instances without modifying the code under test. `createApp()` is a factory (not a singleton) so tests can mount a fresh app per suite. Middleware guards are isolated functions testable in isolation. |

### Trustworthiness

| Attribute | Implementation |
|---|---|
| **Confidentiality** | Passwords are bcrypt-hashed (cost 12) and never returned by queries (excluded via Sequelize `defaultScope`). Password reset tokens are stored as bcrypt hashes — raw value sent by email only, never logged. Session cookies are `httpOnly`, `SameSite=lax`, `secure` in production. SMTP credentials and session secrets are env-var-only. No sensitive data in error responses or log entries. |
| **Accountability** | All security-significant actions are double-recorded: structured logger (`logger.audit`) for real-time observability and `AuditLog` DB table for persistent append-only history. Covered events: REGISTER, LOGIN (success/failure), LOGOUT, PASSWORD_RESET, NOTE_CREATE/UPDATE/DELETE/REASSIGN, ATTACHMENT_UPLOAD/DELETE, SHARE_LINK_CREATE/REGENERATE/REVOKE, AUTHZ_DENY. |
| **Authenticity** | Passport.js local strategy with bcrypt verification. Session integrity via `express-session` with signed cookies. CSRF protection on all state-changing requests via `csrf-sync` (synchroniser token pattern). Share link tokens are 48 random bytes (cryptographically unpredictable via `crypto.randomBytes`). |

### Reliability

| Attribute | Implementation |
|---|---|
| **Availability** | Rate limiting via `express-rate-limit`: 10 attempts per 15 min on login (with `skipSuccessfulRequests`), 300 requests per 15 min general. Body parsers have a 256 KB limit. File uploads are bounded by `UPLOAD_MAX_SIZE_BYTES` (default 5 MB). Graceful shutdown closes DB pool and HTTP server on SIGTERM/SIGINT. |
| **Integrity** | Every trust boundary applies canonicalise → sanitise → validate (FIASSE S6.4.1): usernames are lowercased and pattern-validated, emails normalised, UUIDs validated via `express-validator` `isUUID()`, rating values clamped server-side. The **Derived Integrity Principle** is enforced throughout: `userId` is always taken from `req.user.id` (session), never from request body. **Request Surface Minimisation**: routes destructure only the exact fields they expect. All DB queries use Sequelize parameterised queries — no raw SQL interpolation. |
| **Resilience** | Specific error handling at each service boundary with HTTP status codes (403, 404, 409). `errorHandler` middleware catches all unhandled errors, logs full context internally, and returns a sanitised message to the client. File deletion handles `ENOENT` gracefully. Path traversal in file storage is prevented by resolving and validating paths against the upload directory. Audit write failures are caught and logged without breaking the request. Fail-fast startup validation for required env vars in production. |

---

## Security notes

- **CSRF**: All POST forms include `<input type="hidden" name="_csrf" value="...">`. The CSRF token is injected into `res.locals` for every request.
- **Content Security Policy**: Helmet's CSP restricts scripts/styles to `'self'` (no external CDN). No inline scripts anywhere.
- **SQL injection**: All database access goes through Sequelize's parameterised query builder. No raw SQL.
- **XSS**: EJS auto-escapes all `<%= %>` interpolations. The `content` field (which allows line breaks) uses `<%- ... .replace(/\n/g, '<br>') %>` — only newlines are converted, all other HTML is still escaped by the prior EJS escape.
- **Path traversal**: `fileService.getFilePath` and `deleteFile` resolve and validate paths against the upload directory before any file operation.
- **Email enumeration**: Password reset always returns the same message regardless of whether the email is registered.

---

## Known dependency advisories (no upstream patch available at generation time)

These advisories are recorded per FIASSE supply-chain hygiene. Patches had not been released at
generation time; update dependencies as soon as upstream fixes are published.

| Package | Advisory | Runtime impact for this app |
|---|---|---|
| `express` ≤ 4.21.2 | `path-to-regexp` ReDoS (GHSA-9wv6-86v2-598j); `body-parser` DoS; `qs` DoS | ReDoS risk is low — all requests pass through `generalLimiter` (300 req/15 min) which bounds attacker throughput before the regex engine is reached. |
| `express` ≤ 4.21.2 | `send` template injection XSS (GHSA-m6fv-jmcg-4jfg) | This app does not call `res.sendFile` or `express.static` on user-supplied paths. Static assets are served from a fixed `public/` directory only. |
| `sequelize` ≤ 6.37.7 | SQL injection via JSON column cast (GHSA-6457-6jrx-69cr) | This app does not use JSON column cast queries. No `CAST(col AS JSON)` expressions appear anywhere in the codebase. |
| `nodemailer` ≤ 8.0.3 | SMTP injection via `envelope.size` (GHSA-c7w3-x93f-qmm8) | The `envelope.size` field is not set anywhere in `emailService.js`. Only `from`, `to`, and `subject` are supplied, and `to`/`subject` are populated from trusted internal data (stored email address, static string). |
| `sqlite3` ≤ 5.1.7 | `tar` path traversal (multiple CVEs) | Build-time only — `tar` is used during native module compilation, not at runtime. The compiled `.node` binary ships in `node_modules`; `tar` is never executed in production. |

**Recommended action**: run `npm audit` and apply patches as they become available. Pin exact versions
in `package.json` (already done) and commit the updated `package-lock.json` after each remediation.
