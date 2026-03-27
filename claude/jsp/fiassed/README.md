# Loose Notes — Java JSP/Servlet Web Application

A secure, multi-user note-taking web application built with Java Servlets and JSP, applying
**FIASSE/SSEM** securability constraints and targeting **OWASP ASVS Level 2**.

---

## Prerequisites

| Tool | Version |
|------|---------|
| Java JDK | 11 or later |
| Apache Maven | 3.6+ |
| Apache Tomcat | 9.x (Servlet 4.0) |

---

## Setup and Run Instructions

### 1. Clone / unzip the project

```bash
cd loose-notes
```

### 2. Build the WAR

```bash
mvn clean package -DskipTests
```

The WAR file is produced at `target/loose-notes.war`.

### 3. Configure upload directory (optional)

By default files are stored in `./uploads` relative to the working directory.
Override with the JVM system property:

```bash
-Dloosenotes.upload.dir=/var/data/loose-notes/uploads
```

### 4. Configure log directory (optional)

Logs default to `./logs`. Override via:

```bash
-DLOG_DIR=/var/log/loose-notes
```

### 5a. Run with embedded Tomcat (development)

```bash
mvn tomcat7:run
```

Application starts at: `http://localhost:8080/loose-notes`

> **Note:** For production, deploy the WAR to a standalone Tomcat 9 instance with HTTPS configured.

### 5b. Deploy to Tomcat 9 (production)

```bash
cp target/loose-notes.war $CATALINA_HOME/webapps/
$CATALINA_HOME/bin/startup.sh
```

### 6. Database

The application uses **H2** in file mode. The database file is created automatically at
`./data/loosenotes.mv.db` on first startup. The schema (`src/main/resources/schema.sql`)
is applied automatically by `AppContextListener`.

To use a different database (e.g., MySQL 8), update `src/main/resources/db.properties`:

```properties
db.url=jdbc:mysql://localhost:3306/loosenotes?useSSL=true&requireSSL=true
db.username=loosenotes_app
db.password=<strong-password>
```

And add the MySQL driver dependency to `pom.xml`.

### 7. Run tests

```bash
mvn test
```

---

## Application URLs

| URL | Description |
|-----|-------------|
| `/loose-notes/` | Home — redirects to notes or login |
| `/loose-notes/auth/login` | Login |
| `/loose-notes/auth/register` | Registration |
| `/loose-notes/auth/forgot-password` | Password reset request |
| `/loose-notes/notes` | Note list (authenticated) |
| `/loose-notes/notes/create` | Create note |
| `/loose-notes/search` | Full-text note search |
| `/loose-notes/top-rated` | Top-rated public notes |
| `/loose-notes/share/{token}` | Shared note view (unauthenticated) |
| `/loose-notes/admin` | Admin dashboard (Admin role only) |

---

## SSEM Attribute Coverage Summary

The nine SSEM attributes across three pillars are addressed as follows:

### Maintainability Pillar

#### Analyzability
All security-sensitive logic is centralized in dedicated, single-purpose classes:
`PasswordPolicyService` for password rules, `ShareTokenService` for token lifecycle,
`FileService` for upload handling, `CsrfUtil` for CSRF management. Methods are kept
under 30 lines. Route handlers in servlets delegate immediately to the service layer
without inlining business logic. SQL result mapping is isolated in private `mapRow()`
helper methods within each DAO.

#### Modifiability
Security policy is externalized and injectable:
- Password policy (min/max length, blocklist) lives in `PasswordPolicyService` — changing policy requires no controller changes.
- File storage limits (max size, quota, allowed/blocked extensions, upload path) are constants in `FileService` configurable via constructor or system properties.
- Session timeout (30-min idle, 8-hr absolute) is declared in `web.xml`.
- Login lockout threshold and duration are named constants in `UserService`.
- All DAOs receive their connections from `DatabaseManager`, so the database backend is replaceable by changing `db.properties`.

#### Testability
All service classes accept their DAO dependencies via constructor injection, making them
fully mockable without modifying source code. Security controls (CSRF validation,
password policy, magic-byte validation, share token resolution) are isolated in dedicated
classes with no servlet-container dependencies, enabling unit testing without a servlet
container. DAO methods are individually testable against a real H2 in-memory database.

---

### Trustworthiness Pillar

#### Confidentiality
- Passwords are stored exclusively as BCrypt hashes (cost 12); plaintext never persisted or logged.
- Share link tokens are stored as SHA-256 hashes only; the raw token is never persisted.
- Password reset tokens stored as SHA-256 hashes; raw token transmitted once via email link.
- Session tokens never appear in URLs — `<tracking-mode>COOKIE</tracking-mode>` enforced in `web.xml`.
- Private notes are excluded from all queries for non-owner users at the SQL level, not post-query.
- File downloads include `Content-Disposition: attachment` preventing browser execution; content type derived from magic bytes, not client-supplied MIME type.
- Upload storage is outside the web root (configurable via system property); files are named by UUID, never by original filename.

#### Accountability
Structured JSON audit events are emitted to a dedicated `AUDIT` log (via Logback) and
persisted to the `audit_logs` table for all security-sensitive operations:
registration, login (success/failure), logout, note CRUD, visibility changes, share link
creation/access/revocation, file upload/download, admin actions, and password resets.
Events carry: `timestamp`, `event_type`, `actor_id`, `actor_username`, `ip_address`,
`resource_type`, `resource_id`, `outcome`, `detail`. No credential values appear in logs.
The `audit_logs` table has no UPDATE or DELETE methods in `AuditLogDao` (append-only design).

#### Authenticity
- BCrypt password verification uses the same code path for login and profile password re-verification.
- Session token rotation occurs on every successful login (old session invalidated before new session is created) — prevents session fixation.
- Admin role is verified via the `userRole` session attribute (set from the `User.role` DB field) on every admin request, enforced by `AdminFilter` before any servlet code executes.
- Share tokens are opaque: validity cannot be inferred from the token value; the server looks up the hashed value in the database on every request.
- CSRF tokens use `MessageDigest.isEqual` for constant-time comparison, preventing timing-oracle attacks.

---

### Reliability Pillar

#### Availability
- `AuthFilter` and `AdminFilter` short-circuit unauthorized requests before service code runs.
- Login lockout (5 attempts, 15-minute lock) is stored server-side in the `users` table, making it bypass-resistant.
- File uploads are bounded: 10 MB per file, 500 MB per user (checked before reading the stream body).
- Search query minimum length (2 chars) and maximum (500 chars) prevent trivially expensive full-table scans.
- All external DB connections use try-with-resources for deterministic release.
- Share link generation is capped at 20 active links per user.
- HTTP security headers (`X-Content-Type-Options`, `CSP`, `X-Frame-Options`) applied globally by `SecurityHeadersFilter`.

#### Integrity
- **Derived Integrity Principle**: `note.userId` is bound from the authenticated session, never from a form parameter. Share tokens are server-generated. File paths use UUID names, never user-supplied filenames.
- All SQL uses `PreparedStatement` — no string interpolation in queries.
- `Note.Visibility` and `User.Role` are Java enums persisted via `.name()` with DB `CHECK` constraints; invalid values are rejected at both model-binding and DB constraint levels.
- Note ratings enforce a `UNIQUE(note_id, user_id)` constraint in the database as a race-condition-safe duplicate guard (not only application-layer logic).
- Note creation/edit/delete operations validate all fields server-side before persistence.
- File magic-byte validation is applied uniformly in `FileService` regardless of submitted extension or MIME type.

#### Resilience
- `PasswordPolicyService.isOnBlocklist()` catches exceptions and fails open (registration proceeds with a warning log) — blocklist failure does not block legitimate users.
- `AuditService.recordEvent()` catches `AuditLogDao` failures and logs them at ERROR without rethrowing — the originating operation completes even if the audit sink is temporarily unavailable.
- Validation failures in servlets forward back to the form with preserved (safe) input and a clear error message; no silent data loss.
- `DatabaseManager` schema initialization is idempotent via `CREATE TABLE IF NOT EXISTS`.
- File upload failures after partial DB commit log orphaned file references for operator cleanup rather than re-exposing the failed note.
- If `FileService` storage path is unavailable, the upload fails with a user-friendly error and the note is not left with a dangling attachment reference.

---

## Security Features Summary

| Feature | Implementation |
|---------|---------------|
| Password hashing | BCrypt cost-12 via jBCrypt |
| CSRF protection | Synchronizer token pattern, `CsrfFilter` global |
| Session security | HttpOnly + Secure + SameSite=Lax cookies; rotation on login |
| Input validation | `ValidationUtil` + service-layer checks at every trust boundary |
| SQL injection prevention | Parameterized queries (`PreparedStatement`) throughout |
| XSS prevention | `<c:out>` / `fn:escapeXml` in all JSP views |
| File upload security | Magic-byte validation, UUID storage names, out-of-web-root storage, executable type blocklist |
| Access control | AuthFilter, AdminFilter, per-operation ownership checks in services |
| Audit logging | Structured JSON to dedicated log + DB table; append-only |
| Security headers | CSP, X-Content-Type-Options, X-Frame-Options, HSTS, Referrer-Policy |
| Share link security | Cryptographically random 128-bit tokens, server-side hash storage, immediate revocation |
| Rate limiting | Login lockout, share link cap, file size/quota limits |

---

## ASVS Level 2 Coverage

The implementation targets OWASP ASVS Level 2 across all sixteen application features.
Key ASVS chapters covered: V2 (Authentication), V3 (Session Management), V4 (Access Control),
V5 (Input Validation), V7 (Logging and Error Handling), V9 (Communication),
V11 (Business Logic), V12 (File and Resources), V14 (Configuration).

---

## Open Gaps (from PRD)

The following items from the enhanced PRD are architectural stubs or deferred for v2:

- **GAP-01**: No MFA — interface point defined, implementation deferred
- **GAP-02**: Breach password list is a local blocklist (HIBP API integration deferred)
- **GAP-03**: No email verification at registration — logged as accepted risk for v1
- **GAP-04**: `IFileScanService` interface stub present; AV scanning deferred
- **GAP-07**: No account deletion / right-to-erasure endpoint
- **GAP-08**: Audit log append-only enforced at DAO layer (no UPDATE/DELETE methods); DB-level enforcement requires DBA role configuration
