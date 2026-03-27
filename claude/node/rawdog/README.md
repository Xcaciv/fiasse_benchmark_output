# Loose Notes

A multi-user note-taking web application built with Node.js and Express.

## Features

- User registration, login, and password reset
- Create, edit, delete notes with public/private visibility
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Share notes via unique links (no login required for viewers)
- Rate notes (1-5 stars) with optional comments
- Search notes by keyword
- Top Rated notes page
- Admin dashboard: user management, note reassignment, activity logs
- User profile management

## Setup

1. **Install dependencies**
   ```bash
   npm install
   ```

2. **Configure environment**
   ```bash
   cp .env.example .env
   # Edit .env and set SESSION_SECRET to a random string
   ```

3. **Create required directories**
   ```bash
   mkdir -p data uploads
   ```

4. **Start the application**
   ```bash
   npm start
   # or for development with auto-reload:
   npm run dev
   ```

5. **Open in browser**
   Navigate to http://localhost:3000

## Default Admin Account

On first run, a default admin is created:
- Email: `admin@example.com`
- Password: `Admin@123`

Change the admin password after first login.

## File Upload Limits

- Max file size: 10 MB
- Allowed types: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG

## Password Reset (Demo Mode)

Emails are not sent. The reset link is printed to the server console:
```
--- PASSWORD RESET EMAIL ---
To: user@example.com
Reset link: http://localhost:3000/auth/reset-password?token=...
----------------------------
```

## Tech Stack

- Express.js 4.x
- SQLite via Sequelize ORM
- Passport.js (Local Strategy) + express-session
- bcryptjs for password hashing
- csurf for CSRF protection
- Multer for file uploads
- EJS templating with Bootstrap 5
- winston for logging
