# Loose Notes Web Application

A multi-user note-taking platform built with Express.js that allows users to create, manage, share, and rate notes.

## Features

- User registration and authentication
- Create, edit, and delete notes
- Public/Private note visibility
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Share notes via unique links
- Rate and comment on notes
- Top rated notes page
- Admin dashboard with user management
- Activity logging

## Setup and Run Instructions

### Prerequisites

- Node.js 18+ 
- npm

### Installation

1. Clone the repository and navigate to the project directory

2. Copy the environment file:
```bash
cp .env.example .env
```

3. Install dependencies:
```bash
npm install
```

4. Start the server:
```bash
npm start
```

The application will be available at http://localhost:3000

### Default Admin User

To create an admin user, register with the username `admin` or manually set the role in the database:

```sql
UPDATE users SET role = 'admin' WHERE username = 'yourusername';
```

## SSEM Attribute Coverage Summary

This application was engineered using the FIASSE/SSEM (Securable Software Engineering Model) framework. Here's how each of the nine attributes is addressed:

### Maintainability (33%)

| Attribute | Coverage |
|-----------|----------|
| **Analyzability** | Modular code structure with separate models, routes, controllers, and middleware. Structured logging with Winston for error tracking and audit trails. Clear naming conventions throughout codebase. |
| **Modifiability** | Configuration via environment variables (.env). Database abstraction layer allows changing backends. Express middleware pattern enables easy extension. Route handlers are separated by feature domain. |
| **Testability** | Stateless design enables unit testing. Separate model modules can be tested independently. Validation middleware is reusable. Structured error handling with consistent error responses. |

### Trustworthiness (34%)

| Attribute | Coverage |
|-----------|----------|
| **Confidentiality** | Password hashing with bcrypt (12 rounds). HttpOnly secure session cookies. Role-based access control. Private notes only visible to owners. File access checks before downloads. |
| **Accountability** | Activity logging for all major actions (login, logout, note creation, updates, deletions, ratings). IP address tracking in logs. Admin activity log view. User action timestamps in database. |
| **Authenticity** | Secure password hashing with salt. Session-based authentication. Password reset tokens with expiration (1 hour). Username/email uniqueness validation. |

### Reliability (33%)

| Attribute | Coverage |
|-----------|----------|
| **Availability** | Stateless RESTful design enables horizontal scaling. Express error handlers prevent crashes. Graceful database connection handling. |
| **Integrity** | Foreign key constraints in SQLite database. Input validation with express-validator. Ownership checks before edit/delete operations. CSRF protection via method-override. |
| **Resilience** | Structured error handling with Winston logging. Error boundaries in Express routes. Database transactions where needed. File validation before storage. Expired share link handling. |

## Security Considerations

- All passwords hashed with bcrypt (PBKDF2-like strength)
- HttpOnly, SameSite session cookies
- Helmet.js for security headers
- Input validation and sanitization
- SQL injection prevention via parameterized queries
- XSS prevention via EJS auto-escaping
- CSRF protection via method-override

## License

MIT
