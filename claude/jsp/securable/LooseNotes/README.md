# LooseNotes — Java JSP/Servlet Web Application

A secure, multi-user note-taking platform built with Java 17, Jakarta EE 5 (Servlet/JSP), and an embedded H2 database. Generated with FIASSE/SSEM securable engineering constraints applied throughout.

---

## Quick Start

### Prerequisites

| Requirement | Version |
|-------------|---------|
| JDK         | 17+     |
| Maven       | 3.8+    |
| Tomcat      | 10.1.x (optional — embedded runner available) |

### Run (embedded Tomcat)

```bash
cd LooseNotes
mvn package tomcat10:run
```

Application starts at `http://localhost:8080/loose-notes`

### Run (external Tomcat 10.1)

```bash
mvn package
cp target/loose-notes.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```

### Build and test

```bash
mvn test          # Run all unit tests
mvn package       # Compile, test, and package WAR
```

---

## Configuration

All application settings live in `src/main/resources/app.properties`.

| Property | Default | Description |
|----------|---------|-------------|
| `db.url` | H2 file-based | JDBC URL. Change to MySQL/PostgreSQL for production |
| `upload.dir` | `./uploads` | Directory for uploaded attachments |
| `upload.maxFileSize` | `5242880` (5 MB) | Maximum file upload size in bytes |
| `rateLimit.login.maxAttempts` | `5` | Max login attempts per IP per window |
| `rateLimit.login.windowSeconds` | `300` | Rate-limit window in seconds (5 min) |
| `token.resetExpireSeconds` | `3600` | Password reset token lifetime (1 hour) |
| `session.timeoutSeconds` | `1800` | Idle session timeout (30 min) |

### Production checklist

- [ ] Set `db.url`, `db.username`, `db.password` to a production database
- [ ] Set `upload.dir` to an absolute path outside the web root
- [ ] Enable `Strict-Transport-Security` header in `SecurityHeadersFilter.java`
- [ ] Set `<secure>true</secure>` in `web.xml` `<cookie-config>` when running behind TLS
- [ ] Set `app.baseUrl` to the public HTTPS URL for share links
- [ ] Configure real email sending (replace stub in `PasswordResetServlet.java`)

---

## Project Structure

```
LooseNotes/
├── pom.xml
├── src/
│   ├── main/
│   │   ├── java/com/loosenotes/
│   │   │   ├── config/          AppContextListener (DI wiring)
│   │   │   ├── dao/             Database access objects (JDBC + PreparedStatement)
│   │   │   ├── filter/          Security filters (auth, CSRF, headers)
│   │   │   ├── model/           Domain entities
│   │   │   ├── service/         Business logic layer
│   │   │   ├── servlet/         HTTP controllers (Jakarta Servlet)
│   │   │   └── util/            Security utilities (password, tokens, CSRF, validation)
│   │   ├── resources/
│   │   │   ├── app.properties   Application configuration
│   │   │   ├── db/schema.sql    Database schema (CREATE TABLE IF NOT EXISTS)
│   │   │   └── logback.xml      Structured logging configuration
│   │   └── webapp/
│   │       ├── WEB-INF/
│   │       │   ├── web.xml      Servlet/filter declarations, session config
│   │       │   └── jsp/         View templates (JSTL 2.0 with c:out encoding)
│   │       ├── css/site.css     Minimal custom styles
│   │       └── index.jsp        Root redirect
│   └── test/
│       └── java/com/loosenotes/
│           ├── service/         UserServiceTest (Mockito-based)
│           └── util/            PasswordUtil, Validation, InputSanitizer, RateLimiter tests
```

---

## Default User Accounts

On first startup the schema is initialized. To create an admin account:

1. Register a regular account via `/auth/register`
2. Connect to the H2 console at `http://localhost:8080/loose-notes` (or use `h2-console`) and run:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'yourusername';
```

---

## SSEM Attribute Coverage Summary

The nine SSEM (Securable Software Engineering Model) attributes are addressed as follows:

### Maintainability Pillar

#### Analyzability
- All service and DAO methods are ≤ 30 lines of code with single responsibilities.
- Cyclomatic complexity is kept below 10 by extracting private helper methods.
- Trust boundary crossings (servlet entry points) are annotated with comments explaining *why* validation occurs at that point.
- `AuditLog.EventType` and `Note.Visibility` enums prevent ambiguous string-based logic.

#### Modifiability
- Three-tier architecture (Servlet → Service → DAO) ensures no layer reaches across tiers.
- All services are wired in `AppContextListener` via constructor injection — implementations can be swapped without modifying business logic.
- Database credentials, rate-limit thresholds, file size limits, and token expiry are all externalized in `app.properties`.
- Security-sensitive logic (password hashing, CSRF token management, audit logging) is centralized in dedicated classes (`PasswordUtil`, `CsrfUtil`, `AuditService`) — never duplicated inline.

#### Testability
- Services depend on injected DAO interfaces; `UserServiceTest` exercises the full auth/registration logic using Mockito mocks without requiring a database or servlet container.
- All utility classes (`PasswordUtil`, `ValidationUtil`, `InputSanitizer`, `RateLimiter`) are pure functions with no side effects — fully testable in isolation.
- `DatabaseManager.setInstance()` supports replacing the singleton in integration tests.

---

### Trustworthiness Pillar

#### Confidentiality
- Passwords are hashed with BCrypt (cost factor 12) via `PasswordUtil`; cleartext passwords are cleared from memory (`Arrays.fill`) immediately after hashing or verification.
- The `User.toString()` method excludes `passwordHash` to prevent accidental logging.
- Password reset tokens are stored as SHA-256 hashes only; the raw token travels only via email (never persisted).
- File attachment stored names are UUIDs; original filenames are stored only in the database for display and are always output-encoded.
- IP addresses in audit logs are anonymized to the `/24` subnet prefix.
- No secrets, credentials, or sensitive data appear in log messages.

#### Accountability
- A dedicated audit trail (`AuditService`, `audit_logs` table, `loose-notes-audit.log`) records all security-sensitive actions: login, logout, failed login, registration, password changes/resets, note mutations, admin operations, share link creation/revocation, file uploads/deletions, and ratings.
- Every audit entry records: userId (or "anon"), event type, detail, anonymized IP, and timestamp.
- The audit log file is append-only; no `DELETE` or `UPDATE` methods are exposed by `AuditLogDao`.
- Audit write failures are non-fatal (warns rather than propagates) to avoid blocking primary operations.

#### Authenticity
- Session fixation is prevented: `AuthServlet` invalidates the old session and creates a fresh one on every successful login.
- CSRF protection uses the synchronizer-token pattern (`CsrfFilter`, `CsrfUtil`) with a 128-bit random session-bound token on all state-changing requests (POST/PUT/DELETE).
- Share link tokens are 256-bit cryptographically random hex strings (`SecureTokenUtil` via `java.security.SecureRandom`).
- Constant-time string comparison (`MessageDigest.isEqual`) is used for CSRF token validation to prevent timing attacks.
- Admin role is enforced at two layers: `AuthenticationFilter` (URL-based) and each `AdminServlet` handler (method-level re-check).

---

### Reliability Pillar

#### Availability
- Login rate limiting (`RateLimiter`, 5 attempts / 5-minute window per IP) protects authentication endpoints from brute-force.
- Registration rate limiting (3 attempts / 10-minute window per IP) is configurable.
- Session idle timeout is enforced (30 minutes, configurable in `app.properties`).
- File upload size is enforced at the servlet multipart config level and independently in `AttachmentService`.
- `RateLimiter` uses `ConcurrentHashMap` for thread-safe operation under concurrent requests.

#### Integrity
- **Derived Integrity Principle**: `userId` is always taken from the authenticated session — never from the request body. This prevents privilege escalation via parameter tampering.
- **Request Surface Minimization**: servlets extract only the specific named parameters they expect; all other request data is ignored.
- All SQL uses `PreparedStatement` exclusively — no string concatenation in any query.
- Input canonicalization → sanitization → validation is applied at every servlet entry point:
  - `InputSanitizer` removes null bytes and control characters.
  - `ValidationUtil` enforces format rules (email, username, rating range, ID bounds).
  - `Note.Visibility` and `Role` enums enforce constrained values.
- Output encoding uses JSTL `<c:out>` throughout all JSP views — no raw `${...}` expressions for user-supplied data.
- Database schema includes `CHECK` constraints on `stars` (1–5) and `role` (`USER`/`ADMIN`) as defense-in-depth.

#### Resilience
- All JDBC connections are opened in try-with-resources blocks; no connection or ResultSet leaks.
- `AttachmentService.saveAttachment()` deletes the uploaded file from disk if the subsequent database insert fails, preventing orphaned files.
- Exception handling uses specific types: `ServiceException` for domain violations, `SQLException` for database errors, `IOException` for file I/O — no bare `catch(Exception)` in business logic.
- The `AuditService.record()` method degrades gracefully: if the database write fails, it logs a warning and continues rather than aborting the primary operation.
- Partial file uploads are cleaned up on failure.
- Unknown visibility strings from form inputs are silently normalized to `PRIVATE` (safe default).

---

## Technology Stack

| Component | Technology | Version | Rationale |
|-----------|-----------|---------|-----------|
| Runtime | Java | 17 LTS | Long-term support; modern language features |
| Web container | Jakarta Servlet/JSP | 5.0 / 3.0 | Jakarta EE 9+ namespace; Tomcat 10.1 compatible |
| Template engine | JSTL | 2.0 | Auto-escaping `<c:out>` prevents XSS |
| Database | H2 (embedded) | 2.2.224 | Zero-config for development; swap-in for MySQL/PG |
| Password hashing | BCrypt (favre-lib) | 0.10.2 | Adaptive cost; low CVE exposure; actively maintained |
| Logging | SLF4J + Logback | 2.0.12 / 1.5.3 | Structured logging; replaceable facade |
| Testing | JUnit 5 + Mockito | 5.10.2 / 5.11.0 | Constructor-injection-friendly mocking |

---

## Security Controls Summary (ASVS Level 1)

| Control | Implementation |
|---------|---------------|
| Password hashing | BCrypt cost 12 (`PasswordUtil`) |
| Password policy | 8–128 characters (`PasswordUtil.meetsPolicy`) |
| Brute-force protection | IP-based rate limiting (`RateLimiter`) |
| Session fixation | Session invalidated on login (`AuthServlet`) |
| CSRF | Synchronizer token (`CsrfFilter`, `CsrfUtil`) |
| SQL injection | PreparedStatement only (all DAO classes) |
| XSS | `<c:out>` in all JSP views + CSP header |
| Clickjacking | `X-Frame-Options: DENY` header |
| Password reset | Time-limited (1h), single-use, hash-stored token |
| Audit trail | Structured append-only log + database table |
| File upload | MIME validation, UUID filename, size limit |
| Access control | Session-based auth + admin role checks |
| Input validation | Canonicalize → sanitize → validate at every boundary |
