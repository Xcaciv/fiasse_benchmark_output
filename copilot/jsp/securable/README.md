# Loose Notes — JSP/Servlet Web Application

A multi-user note-taking platform built with Java Servlets + JSP, engineered for
securability using the FIASSE/SSEM framework.

---

## Tech Stack

| Layer        | Technology                                 |
|--------------|--------------------------------------------|
| Runtime      | Java 11 + Jakarta EE (Servlet 4.0 / JSP)  |
| Build        | Maven 3.8+                                 |
| Database     | SQLite (embedded, via HikariCP pool)       |
| Auth         | Session-based + BCrypt (jBCrypt)           |
| File Upload  | Apache Commons FileUpload                  |
| Logging      | SLF4J + Logback                            |
| Server       | Embedded Jetty (dev) or any Servlet 4 WAR  |

---

## Requirements

- Java 11+
- Maven 3.8+

---

## Setup & Run

### Option A — Embedded Jetty (Development)

```bash
# Clone / enter project directory
cd <project-root>

# Build and run with embedded Jetty on http://localhost:8080
mvn jetty:run
```

The database and uploads are created automatically at `~/.loosenotes/`.

### Option B — Deploy WAR

```bash
mvn clean package
# Deploy target/loose-notes-1.0-SNAPSHOT.war to Tomcat/Jetty/WildFly
```

### Option C — Custom DB path

Edit `src/main/webapp/WEB-INF/web.xml` context parameters:

```xml
<context-param>
    <param-name>db.path</param-name>
    <param-value>/path/to/your/loosenotes.db</param-value>
</context-param>
<context-param>
    <param-name>upload.dir</param-name>
    <param-value>/path/to/your/uploads</param-value>
</context-param>
```

### Running Tests

```bash
mvn test
```

---

## Default Behaviour

| URL              | Description                          |
|------------------|--------------------------------------|
| `/`              | Redirects to `/login` or `/notes`    |
| `/register`      | Create a new account                 |
| `/login`         | Sign in                              |
| `/notes`         | List your notes                      |
| `/notes/new`     | Create a note                        |
| `/notes/{id}`    | View a note                          |
| `/search`        | Search notes                         |
| `/top-rated`     | Top-rated public notes               |
| `/share/{token}` | Public share link view               |
| `/profile`       | Edit profile / change password       |
| `/admin`         | Admin dashboard (ADMIN role only)    |

---

## SSEM Attribute Coverage Summary

### Maintainability

| Attribute        | How It Is Addressed |
|------------------|---------------------|
| **Analyzability** | All methods ≤ 30 LoC; cyclomatic complexity < 10 throughout. Clear, intent-revealing names (`requireNoteOwner`, `validateFileExtension`). Trust boundaries are annotated with inline comments. |
| **Modifiability** | Strict separation: DAO interfaces → implementations, Service interfaces → implementations, Servlets depend on service interfaces only. All wiring is centralized in `AppContextListener`. Context parameters externalize configuration (DB path, upload dir, max size). No static mutable state. |
| **Testability** | Every public interface (`UserDao`, `NoteDao`, `UserService`, `NoteService`, `FileService`) is injectable via constructor. Tests in `src/test/` demonstrate Mockito-compatible design. `ValidationUtilTest` and `PasswordUtilTest` run without a server. |

### Trustworthiness

| Attribute         | How It Is Addressed |
|-------------------|---------------------|
| **Confidentiality** | Passwords are hashed with BCrypt (work factor 12); plaintext never stored, logged, or surfaced. `User.toString()` omits `passwordHash`. Error pages show no stack traces or internal detail. File downloads validate path containment to prevent traversal. |
| **Accountability** | `AuditLogger` writes structured entries to both the `audit_log` DB table and the `AUDIT` SLF4J logger for every security-sensitive event (register, login, logout, note CRUD, attachment upload/delete, share link generate/revoke, admin reassign). Entries include actor, action, resource, IP, and outcome — no sensitive data. |
| **Authenticity** | Session-based authentication; session is invalidated and recreated on login to prevent session fixation. `CsrfFilter` validates a `SecureRandom`-generated CSRF token on all POST/PUT/DELETE requests. `AuthFilter` enforces authentication on protected URL patterns and role-checks `/admin/*`. |

### Reliability

| Attribute    | How It Is Addressed |
|--------------|---------------------|
| **Availability** | HikariCP connection pool limits DB concurrency (max 10 connections). File upload size is capped at 10 MB (configurable). HTTP session timeout is 30 minutes. Security headers (`X-Frame-Options`, `X-Content-Type-Options`, etc.) set by `EncodingFilter` on every response. |
| **Integrity** | All SQL uses `PreparedStatement` — no concatenated queries. Input passes through a canonicalize → sanitize → validate pipeline in `ValidationUtil`. File uploads are extension-allowlisted and stored under UUIDs to prevent path traversal. CSRF tokens use constant-time comparison. JSP output uses `<c:out>` exclusively for HTML encoding. |
| **Resilience** | All JDBC operations use try-with-resources. `AuditLogger` catches and logs persistence failures without propagating them. `FileServiceImpl` validates path containment before any file I/O. Error pages (`error.jsp`) catch all servlet exceptions gracefully. `AppContextListener` closes the connection pool on shutdown. |

---

## Project Structure

```
src/
├── main/
│   ├── java/com/loosenotes/
│   │   ├── audit/         AuditLogger
│   │   ├── context/       AppContextListener (DI wiring)
│   │   ├── dao/           DAO interfaces + JDBC implementations
│   │   ├── filter/        EncodingFilter, CsrfFilter, AuthFilter
│   │   ├── model/         Immutable domain models
│   │   ├── service/       Service interfaces + implementations
│   │   ├── servlet/       HTTP servlet controllers
│   │   └── util/          DatabaseManager, PasswordUtil, ValidationUtil, CsrfUtil, HtmlEncoder
│   ├── resources/
│   │   ├── schema.sql     Idempotent DDL
│   │   └── logback.xml    Structured audit + app logging
│   └── webapp/
│       ├── WEB-INF/
│       │   ├── web.xml    Servlet/filter mapping + context params
│       │   └── jsp/       JSP views (auth/, note/, admin/, user/, layout/)
│       ├── css/styles.css
│       └── index.jsp
└── test/
    └── java/com/loosenotes/util/
        ├── ValidationUtilTest.java
        └── PasswordUtilTest.java
```

---

## License

CC-BY-4.0 — FIASSE/SSEM Securable Software Engineering Model
