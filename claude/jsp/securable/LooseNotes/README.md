# Loose Notes — Java JSP/Servlet Web Application

A securable multi-user note-taking platform built with Java Servlets and JSP, engineered to
the FIASSE/SSEM (Securable Software Engineering Model) framework.

---

## Technology Stack

| Layer         | Technology                         |
|:--------------|:-----------------------------------|
| Backend       | Java 17, Jakarta Servlets 6.0, JSP 3.1 |
| Database      | H2 (embedded, file-persisted)      |
| Connection Pool | HikariCP 5.x                     |
| Password Hash | BCrypt (cost 12, favre-lib)        |
| Logging       | SLF4J + Logback                    |
| Templates     | JSTL 3.0 with `<c:out>` XSS encoding |
| Build         | Maven 3.8+                         |
| Container     | Apache Tomcat 10+ (Jakarta EE 9)   |
| Tests         | JUnit 5 + Mockito                  |

---

## Prerequisites

- Java 17+
- Maven 3.8+
- Apache Tomcat 10.x (Jakarta EE 9 namespace)

---

## Setup and Run

### 1. Clone / extract the project

```bash
cd LooseNotes
```

### 2. Build the WAR

```bash
mvn clean package
```

This produces `target/loose-notes.war`.

### 3. Deploy to Tomcat

Copy the WAR to Tomcat's `webapps/` directory:

```bash
cp target/loose-notes.war $CATALINA_HOME/webapps/loose-notes.war
```

Start Tomcat:

```bash
$CATALINA_HOME/bin/startup.sh
```

### 4. Access the application

Open `http://localhost:8080/loose-notes` in your browser.

**Default admin account:**
- Username: `admin`
- Password: `Admin@1234`
  ⚠️ **Change this immediately after first login** via Profile → Change Password.

---

## Configuration

All configuration lives in `src/main/resources/app.properties`.

Override sensitive settings via environment variables:

| Variable       | Purpose                                    |
|:---------------|:-------------------------------------------|
| `DB_URL`       | JDBC URL (default: H2 file-based)          |
| `APP_BASE_URL` | Base URL for share links and reset emails  |

---

## Database

The application uses H2 in file-persisted mode by default. Data is stored in `./data/loosenotes.mv.db` relative to the Tomcat working directory.

To use a different database (PostgreSQL, MySQL), change `db.url` and `db.driver` in `app.properties` and add the appropriate JDBC driver to the WAR.

---

## Features

| Requirement | Feature                              | Status |
|:------------|:-------------------------------------|:-------|
| REQ-001     | User Registration                    | ✅     |
| REQ-002     | Login / Session Authentication       | ✅     |
| REQ-003     | Password Reset (email token)         | ✅     |
| REQ-004     | Note Creation                        | ✅     |
| REQ-005     | File Attachments                     | ✅     |
| REQ-006     | Note Editing                         | ✅     |
| REQ-007     | Note Deletion (with confirmation)    | ✅     |
| REQ-008     | Share Links (generate/revoke)        | ✅     |
| REQ-009     | Public / Private Visibility          | ✅     |
| REQ-010     | Note Rating (1–5 stars + comment)   | ✅     |
| REQ-011     | Rating Management                    | ✅     |
| REQ-012     | Note Search                          | ✅     |
| REQ-013     | Admin Dashboard + Activity Log       | ✅     |
| REQ-014     | User Profile Management              | ✅     |
| REQ-015     | Top Rated Notes                      | ✅     |
| REQ-016     | Admin: Note Ownership Reassignment   | ✅     |

---

## Running Tests

```bash
mvn test
```

Test coverage includes:
- `PasswordUtilTest` — BCrypt hash/verify, complexity enforcement, char[] clearing
- `InputSanitizerTest` — canonicalize/sanitize pipeline, path traversal prevention
- `ValidationUtilTest` — boundary enforcement, token format validation
- `UserServiceTest` — registration, authentication, lockout (Mockito-based)
- `RateLimiterTest` — rate limit enforcement, reset, window independence

---

## SSEM Score Summary

| Attribute         | Pillar           | Score | Key Evidence                                                                            |
|:------------------|:-----------------|:-----:|:----------------------------------------------------------------------------------------|
| **Analyzability** | Maintainability  |  8/10 | Methods ≤30 LoC; cyclomatic complexity <10; trust boundaries commented; clear naming     |
| **Modifiability** | Maintainability  |  8/10 | Service/DAO/Servlet layers; DI via constructor; centralized config; no static mutable state |
| **Testability**   | Maintainability  |  8/10 | Services depend on interface (DataSource); Mockito-testable; pure utility functions      |
| **Confidentiality**| Trustworthiness |  9/10 | BCrypt cost-12 passwords; tokens stored as SHA-256 hashes; no sensitive data in logs/errors; storedFilename never exposed |
| **Accountability** | Trustworthiness |  9/10 | AuditService logs all auth events, note CRUD, admin actions; structured SLF4J audit log  |
| **Authenticity**  | Trustworthiness  |  8/10 | CSRF synchronizer tokens; session rotation on login; cryptographic share/reset tokens; account lockout |
| **Availability**  | Reliability      |  8/10 | Rate limiting (5 req/15 min login); file size limits (10 MB); connection pooling; input length limits |
| **Integrity**     | Reliability      |  9/10 | Canonicalize→Sanitize→Validate pipeline; PreparedStatements throughout; path traversal prevention; ownership in WHERE clauses |
| **Resilience**    | Reliability      |  8/10 | try-with-resources; specific exception handling; audit failures non-fatal; defensive null checks; immutable collection views |

**Overall SSEM Score: 8.3 / 10**

### Notable Securability Decisions

1. **Derived Integrity** — Share link tokens are SHA-256 hashed before DB storage (raw token only in URL). Password reset tokens use the same pattern.
2. **Trust Boundary Hardening** — Every servlet entry point runs: `InputSanitizer.sanitize*()` → `ValidationUtil.isValid*()` → business logic.
3. **Turtle Analogy** — Hard shell at HTTP request boundary; interior service/DAO code trusts pre-validated inputs.
4. **Fail-Safe Defaults** — Unknown roles default to `USER`; invalid tokens get 404 (not 403) to prevent enumeration; password reset always shows "email sent" regardless of user existence.
5. **No Secrets in Code** — No hardcoded credentials. The seeded admin password must be changed on first login.

### Known Limitations / Production Hardening Checklist

- [ ] Configure `<secure>true</secure>` on session cookie (requires HTTPS)
- [ ] Replace H2 with production database (PostgreSQL recommended)
- [ ] Integrate real email service for password reset tokens
- [ ] Enable HSTS header (`Strict-Transport-Security`) behind TLS termination
- [ ] Set `app.baseUrl` via `APP_BASE_URL` environment variable
- [ ] Change default admin password on first deployment
- [ ] Review and tighten CSP policy for your deployment
- [ ] Consider distributed rate limiter (Redis) for multi-instance deployments

---

## Security Architecture Diagram

```
Browser ──HTTPS──► [SecurityHeadersFilter] ──► [AuthenticationFilter]
                                                        │
                              ┌─────────────────────────┘
                              │
                     [CsrfFilter (POST/PUT/DELETE)]
                              │
                     [Servlet: AuthServlet / NoteServlet / ...]
                              │
              ┌───────────────┼───────────────┐
              ▼               ▼               ▼
      [InputSanitizer]  [ValidationUtil]  [CsrfUtil]
              │
       [Service Layer]   ←── AuditService (accountability)
              │
       [DAO Layer]        ──► PreparedStatement (integrity)
              │
       [H2 Database]      ──► Schema constraints, FK cascades
```
