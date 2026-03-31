# Loose Notes Web Application

A multi-user note-taking platform built with Java JSP and Servlets, implementing FIASSE/SSEM securable engineering principles.

## Technical Stack

- **Java** 11
- **JSP & Servlets** (Jakarta EE)
- **Database**: H2 (in-memory/file)
- **Authentication**: Custom session-based with BCrypt password hashing
- **Build Tool**: Maven

## Features

- User registration and authentication
- Create, edit, delete notes
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Public/Private note visibility
- Share notes via unique links
- 1-5 star ratings with comments
- Top rated notes page
- Admin dashboard with user management
- Note ownership reassignment
- Activity logging

## Setup and Run

### Prerequisites
- Java 11 or higher
- Maven 3.6+

### Build and Run

```bash
# Build the project
mvn clean package

# Run the application
mvn jetty:run
```

The application will start at `http://localhost:8080`

### Default Admin User

After first run, create an admin user by manually setting their role to `ADMIN` in the database, or use the registration and modify the database directly.

## Project Structure

```
src/
├── main/
│   ├── java/com/loosenotes/
│   │   ├── dao/           # Data Access Objects
│   │   ├── filter/       # Servlet Filters (Authentication)
│   │   ├── listener/     # Application Listeners
│   │   ├── model/        # Data Models
│   │   ├── servlet/      # Servlet Controllers
│   │   └── util/         # Utilities
│   └── webapp/
│       ├── WEB-INF/
│       │   └── views/    # JSP Views
│       ├── css/
│       ├── js/
│       └── uploads/      # File attachments storage
└── resources/
    └── logback.xml       # Logging configuration
```

## SSEM Attribute Coverage Summary

### Maintainability (33%)

| Attribute | Implementation |
|-----------|---------------|
| **Analyzability** | Clean separation of concerns with DAO, Servlet, and JSP layers. Meaningful naming conventions. Comprehensive logging for debugging. |
| **Modifiability** | Modular design with separate DAOs for each entity. Configuration in DatabaseUtil. Web.xml for servlet configuration. |
| **Testability** | Stateless servlet design. DAOs can be tested independently. Clear interfaces between layers. |

### Trustworthiness (34%)

| Attribute | Implementation |
|-----------|---------------|
| **Confidentiality** | BCrypt password hashing with salt. Session-based authentication. Role-based access control (USER/ADMIN). Ownership checks before edit/delete. |
| **Accountability** | Activity logging for all sensitive actions (login, logout, CRUD operations). IP address capture. Timestamp tracking on all entities. |
| **Authenticity** | User identification via session. Share link tokens for unauthenticated note access. Token expiration for password reset. |

### Reliability (33%)

| Attribute | Implementation |
|-----------|---------------|
| **Availability** | H2 database with auto-server mode. Connection pooling via Apache DBCP2. |
| **Integrity** | Foreign key constraints in database schema. File extension validation. SQL injection prevention via PreparedStatements. |
| **Resilience** | Proper error handling with try-catch. Graceful error pages. Transaction handling in DAOs. File cleanup on note deletion. |

## Security Features

- **Password Hashing**: BCrypt with automatic salt generation
- **SQL Injection Prevention**: Parameterized queries throughout
- **XSS Prevention**: JSP expression language handles encoding
- **Session Management**: Configurable timeout, secure session handling
- **Access Control**: Authentication filter for protected resources
- **Ownership Verification**: Note ownership checks for edit/delete operations
- **File Upload Validation**: Allowed file type verification, size limits
- **Audit Logging**: All authentication and administrative actions logged

## Configuration

### Database
The application uses H2 database. Default configuration in `DatabaseUtil.java`:
- URL: `jdbc:h2:./loosenotes;AUTO_SERVER=TRUE`
- No authentication required for local development

### Session Timeout
Default: 60 minutes (configurable in web.xml)

### File Upload
- Max file size: 10MB
- Allowed types: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG
- Storage location: `src/main/webapp/uploads/`

## License

MIT License
