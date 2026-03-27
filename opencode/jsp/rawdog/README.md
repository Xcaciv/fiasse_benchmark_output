# Loose Notes Web Application

A multi-user note-taking platform built with Java JSP and Servlets that allows users to create, manage, share, and rate notes.

## Features

- **User Authentication**: Register, login, logout, and password reset
- **Note Management**: Create, edit, view, and delete notes
- **File Attachments**: Upload files (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG) up to 10MB
- **Note Sharing**: Generate share links to share notes with anyone
- **Public/Private Notes**: Control visibility of notes
- **Rating System**: Rate notes with 1-5 stars and add comments
- **Search**: Search notes by title or content
- **Top Rated**: View highest-rated public notes
- **Admin Dashboard**: Manage users and reassign note ownership
- **Activity Logging**: Track user actions and admin operations

## Technology Stack

- **Backend**: Java 11, JSP, Servlets
- **Database**: MySQL with JDBC
- **Security**: BCrypt password hashing, Session-based authentication
- **Frontend**: Bootstrap 4, Font Awesome, HTML/CSS/JavaScript
- **Build Tool**: Maven

## Prerequisites

- Java 11 or higher
- Apache Tomcat 9 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher

## Database Setup

1. Create a MySQL database named `loose_notes`:

```sql
CREATE DATABASE loose_notes;
```

2. Update the database configuration in `src/main/resources/database.properties`:

```properties
db.driver=com.mysql.cj.jdbc.Driver
db.url=jdbc:mysql://localhost:3306/loose_notes?useSSL=false&serverTimezone=UTC
db.username=root
db.password=your_password
```

The application will automatically create all required tables on first run.

## Building the Application

1. Navigate to the project directory:

```bash
cd loose-notes
```

2. Build the project using Maven:

```bash
mvn clean package
```

3. The WAR file will be created at `target/loose-notes.war`

## Running the Application

### Option 1: Deploy to Tomcat

1. Copy `target/loose-notes.war` to your Tomcat's `webapps` directory
2. Start Tomcat
3. Access the application at `http://localhost:8080/loose-notes/`

### Option 2: Run with Maven Tomcat Plugin

```bash
mvn tomcat7:run
```

The application will be available at `http://localhost:8080/loose-notes/`

### Option 3: Run with Jetty

```bash
mvn jetty:run
```

The application will be available at `http://localhost:8080/`

## Default Admin Account

On first run, an admin account is automatically created:

- **Username**: admin
- **Email**: admin@loosenotes.com
- **Password**: Admin123!

Please change the password immediately after first login.

## Project Structure

```
loose-notes/
├── pom.xml
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── loosenotes/
│       │           ├── dao/           # Data Access Objects
│       │           ├── filter/        # Servlet Filters
│       │           ├── model/         # Domain Models
│       │           ├── servlet/       # Servlets (Controllers)
│       │           └── util/         # Utility Classes
│       ├── resources/
│       │   └── database.properties   # DB Configuration
│       └── webapp/
│           ├── WEB-INF/
│           │   ├── views/            # JSP Views
│           │   └── web.xml           # Web Configuration
│           ├── css/                  # Stylesheets
│           ├── js/                   # JavaScript
│           └── uploads/              # File Upload Directory
└── README.md
```

## API Routes

| URL | Method | Description |
|-----|--------|-------------|
| `/` | GET | Home page (logged out: landing, logged in: dashboard) |
| `/auth` | GET/POST | Login, logout, register, password reset |
| `/notes` | GET/POST | List, create, edit, delete notes |
| `/search` | GET/POST | Search notes |
| `/top-rated` | GET | View top rated notes |
| `/profile` | GET/POST | User profile management |
| `/upload` | POST | File upload/delete |
| `/ratings` | POST | Submit/update/delete ratings |
| `/share` | GET | View shared note |
| `/admin` | GET/POST | Admin dashboard (admin only) |

## Security Features

- BCrypt password hashing (cost factor 10)
- Session-based authentication
- CSRF protection via session
- SQL injection prevention via PreparedStatements
- XSS prevention via input sanitization
- Role-based access control (USER, ADMIN)
- Ownership verification for edit/delete operations

## File Upload

Allowed file types:
- PDF (.pdf)
- Word Documents (.doc, .docx)
- Text Files (.txt)
- Images (.png, .jpg, .jpeg)

Maximum file size: 10MB

## License

This project is for educational purposes.
