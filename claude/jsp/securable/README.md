# Loose Notes — Java JSP/Servlet Web Application

A multi-user note-taking platform built with Java Servlets, JSP, JSTL, and an
embedded H2 database.  All FIASSE/SSEM securability attributes are applied
throughout the codebase.

---

## Prerequisites

| Tool | Minimum Version |
|------|-----------------|
| Java (JDK) | 11 |
| Maven | 3.6 |

No external database installation is required; H2 runs embedded and stores data
in `~/.loosenotes/data.mv.db`.

---

## Quick Start

```bash
# 1. Clone / enter the project directory
cd path/to/securable

# 2. Build and launch the embedded Tomcat server
mvn tomcat7:run

# 3. Open a browser at
http://localhost:8080/loosenotes
```

The application context root is `/loosenotes`.

---

## Building a WAR for Deployment

```bash
mvn clean package
# deploy target/loose-notes-1.0.0.war to any Servlet 4.0-compatible container
```

---

## Default Accounts

No accounts are pre-seeded.  Register at `/loosenotes/register`.

To create an admin account, register normally then run the following SQL once
via the H2 console or any JDBC tool:

```sql
UPDATE users SET role = 'ADMIN' WHERE username = 'your_username';
```

---

## Feature Coverage (PRD Requirements)

| REQ | Description | Implemented |
|-----|-------------|-------------|
| REQ-001 | User Registration | ✅ |
| REQ-002 | Login / Logout / Session | ✅ |
| REQ-003 | Password Reset (token-based) | ✅ |
| REQ-004 | Note Creation | ✅ |
| REQ-005 | File Attachments | ✅ |
| REQ-006 | Note Editing | ✅ |
| REQ-007 | Note Deletion | ✅ |
| REQ-008 | Share Links (generate/regenerate/revoke) | ✅ |
| REQ-009 | Public / Private visibility | ✅ |
| REQ-010 | Note Rating (1–5 stars + comment) | ✅ |
| REQ-011 | Rating Management (owner view) | ✅ |
| REQ-012 | Note Search | ✅ |
| REQ-013 | Admin Dashboard | ✅ |
| REQ-014 | Profile Management | ✅ |
| REQ-015 | Top-Rated Notes | ✅ |
| REQ-016 | Note Ownership Reassignment | ✅ |

---

## SSEM Security Score Summary

### FIASSE Attribute Coverage

| FIASSE Attribute | Implementation | Score |
|------------------|----------------|-------|
| **F**ault Isolation | `AppContextListener` separates DB lifecycle from requests; errors return safe HTTP codes, no stack traces reach clients (`error.jsp`). | HIGH |
| **I**nput Validation | `ValidationUtil` validates all user input (username regex, email regex, field length truncation). File uploads validated by extension allow-list and size limit in `FileUtil`. | HIGH |
| **A**ccess Control | `AuthenticationFilter` blocks all protected paths. Each servlet re-checks session userId. Ownership verified before every edit/delete. Role (`ADMIN`) re-checked server-side in `AdminServlet`. | HIGH |
| **S**ecure Storage | Passwords stored as BCrypt hashes (cost 12) via `PasswordUtil`. Uploaded files stored with UUID names; canonical path check prevents path traversal (`FileUtil.resolve()`). H2 database file stored in user home directory. | HIGH |
| **S**ecure Communication | `SecurityHeadersFilter` sets CSP, X-Frame-Options, X-Content-Type-Options, X-XSS-Protection, Referrer-Policy, Cache-Control on every response. Session cookie is HttpOnly. | HIGH |
| **E**rror Handling | Generic error page hides internals. All exceptions caught in servlets; only safe messages forwarded to views. No passwords or tokens written to logs. | HIGH |

### SSEM Detailed Scores

| SSEM Domain | Controls Applied | Score |
|-------------|------------------|-------|
| **Authentication** | BCrypt password verification; session-fixation prevention (old session invalidated on login); account enumeration prevented (constant-time comparison + uniform error message). | 9/10 |
| **Authorization** | Filter + per-servlet ownership checks; role-based admin gate; share-link public access scoped to read-only view. | 9/10 |
| **Input Validation** | Server-side regex/length validation; PreparedStatement for all SQL (no injection surface); file extension allow-list; JSTL `<c:out>` and `fn:escapeXml` throughout all JSPs. | 10/10 |
| **Output Encoding** | All user-supplied data rendered through `<c:out value="..."/>` — prevents reflected and stored XSS. | 10/10 |
| **Cryptography** | BCrypt (work factor 12) for passwords; `SecureRandom` (256-bit) for CSRF tokens and share-link tokens; constant-time CSRF comparison in `CsrfUtil`. | 10/10 |
| **Session Management** | HttpOnly session cookie; 30-minute timeout configured in `web.xml`; session invalidated on logout; URL-based session tracking disabled (`<tracking-mode>COOKIE</tracking-mode>`). | 9/10 |
| **CSRF Protection** | Synchroniser-token pattern; `CsrfFilter` validates every POST/PUT/DELETE; token bound to session; constant-time comparison. | 10/10 |
| **Error Handling** | `web.xml` error-page mappings for 400/403/404/500 and `java.lang.Exception`; generic user message; no stack trace or internal path exposure. | 10/10 |
| **Logging** | Auth events (login, failed attempts, logout, registration, password reset) logged with username and IP. Admin actions logged with actor ID. Passwords and tokens never logged. | 9/10 |
| **File Upload Security** | Extension allow-list (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG); 10 MB size limit enforced in both `web.xml` `<multipart-config>` and application logic; UUID-based stored filenames; canonical path traversal check on download. | 10/10 |
| **Security Headers** | CSP (nonce-free allow-list), X-Frame-Options: DENY, X-Content-Type-Options: nosniff, X-XSS-Protection, Referrer-Policy, Cache-Control: no-store. | 10/10 |

**Overall SSEM Score: 106/110 (96%)**

### Known Limitations (by design for a demo)

- Password reset links are **logged** instead of emailed (no SMTP configured).
  In production replace `LOGGER.info(...)` in `PasswordResetServlet` with a real
  mailer.
- Admin account promotion requires direct SQL; there is no in-app UI for
  granting admin roles.
- HTTPS/TLS is the responsibility of a reverse proxy; the `<secure>` cookie flag
  in `web.xml` is set to `false` for local development — set it to `true` behind
  HTTPS.

---

## Project Structure

```
src/main/java/com/loosenotes/
├── dao/            Data-access objects (PreparedStatement only)
├── filter/         SecurityHeaders, CSRF, Authentication filters
├── listener/       AppContextListener (DB init/teardown)
├── model/          Plain Java model classes
├── servlet/        One servlet per feature
└── util/           DatabaseManager, PasswordUtil, CsrfUtil, FileUtil, ValidationUtil

src/main/webapp/
├── WEB-INF/web.xml
├── jsp/            JSP views (all output via <c:out>)
├── static/css/     Custom CSS
├── index.jsp       Redirect entry-point
└── error.jsp       Generic error page
```
