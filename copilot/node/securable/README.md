# Loose Notes

A multi-user note-taking platform built with Node.js, Express.js, and SQLite.

## Setup Instructions

1. **Clone the repository**
   ```bash
   git clone <repo-url>
   cd loose-notes
   ```

2. **Install dependencies**
   ```bash
   npm install
   ```

3. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env and set SESSION_SECRET, SMTP_*, APP_URL
   ```

4. **Start the application**
   ```bash
   # Development (with auto-reload)
   npm run dev

   # Production
   npm start
   ```

   The database is created automatically on first run at the path specified by `DB_PATH`.

5. **First user is automatically promoted to admin.**

---

## SSEM Attribute Coverage Summary

| SSEM Attribute | How It Is Addressed |
|---|---|
| **Analyzability** | All functions are ≤30 LoC with cyclomatic complexity <10. Descriptive names throughout. Comments mark trust boundaries (HTTP entry, DB access, file I/O). Each module has a single responsibility. |
| **Modifiability** | Security logic is centralized in dedicated modules (`authService`, `auditService`, `csrfProtection`, `rateLimiter`). Configuration is externalized to `.env`. Loose coupling via service-layer injection; routes delegate to services, not inline logic. |
| **Testability** | All services expose pure functions with injectable dependencies. Middleware (`authenticate`, `authorize`, `validate`) is isolated and can be tested independently. Rate limiter skips in `NODE_ENV=test`. |
| **Confidentiality** | Passwords hashed with bcrypt (cost 12). `toJSON()` on `User` strips `passwordHash` and `resetToken`. Audit metadata is sanitized to remove sensitive fields before writing. Session cookies are `httpOnly`, `sameSite:strict`, `secure` in production. No secrets in source code. |
| **Accountability** | All security-sensitive actions (login, logout, register, note CRUD, file upload/delete, admin actions) are written to the append-only `AuditLog` table via `auditService.logAction()`. Logs include `actorId`, `action`, `resourceType`, `resourceId`, `ipAddress`, and `createdAt`. |
| **Authenticity** | Sessions are regenerated on login to prevent session fixation. CSRF protection applied globally (excluding public share routes) via `csurf`. Password reset uses bcrypt-hashed tokens with 1-hour expiry. Timing-safe login to prevent user enumeration. |
| **Availability** | Rate limiting on auth endpoints (10/15 min), API endpoints (100/15 min), and file uploads (20/hr) via `express-rate-limit`. Request body size capped at 1 MB. File upload size capped at 10 MB via multer. Helmet sets security headers. |
| **Integrity** | All user input is validated with `express-validator` chains (canonicalize → sanitize → validate). Sequelize ORM with parameterized queries exclusively (no raw SQL). Derived Integrity Principle: visibility defaults server-side; only `['public','private']` accepted from client. Path traversal prevented in `fileService` via `path.resolve` guard. |
| **Resilience** | Specific error handling with HTTP status codes throughout all services and routes. No bare catch-all handlers. DB cascade deletes ensure referential integrity on note deletion. File cleanup handles missing files gracefully (warns, does not throw). `server.js` creates upload/log directories before starting. |
