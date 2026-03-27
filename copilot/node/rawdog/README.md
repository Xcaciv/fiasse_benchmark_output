# Loose Notes

A multi-user note-taking platform built with Node.js and Express.js.

## Features

- **Multi-user authentication** — Register, login, password reset via email
- **Note management** — Create, edit, delete, view notes with public/private visibility
- **File attachments** — Upload PDF, DOC, DOCX, TXT, PNG, JPG files (max 10MB)
- **Ratings & comments** — Rate public notes (1–5 stars) with optional comments
- **Public share links** — Generate unique share links for notes (no auth required to view)
- **Full-text search** — Search your notes and public notes by title/content
- **Top-rated view** — Public notes with ≥3 ratings sorted by average rating
- **Admin dashboard** — View stats, manage users, reassign notes
- **Activity logging** — All key actions are logged for admin review

## Setup

```bash
# 1. Install dependencies
npm install

# 2. Copy environment template (optional — app runs with defaults)
copy .env.example .env

# 3. Start the server
npm start
# or for development with auto-reload:
npm run dev
```

The app will create the SQLite database automatically at `data/loose-notes.db`.

## Default Admin Credentials

| Username | Password   |
|----------|------------|
| admin    | Admin1234! |

> ⚠️ Change the admin password after first login.

## Environment Variables

See `.env.example` for all available configuration options.

| Variable         | Default                    | Description              |
|-----------------|----------------------------|--------------------------|
| `PORT`          | `3000`                     | HTTP port                |
| `SESSION_SECRET`| `loose-notes-secret-key`   | Session signing secret   |
| `EMAIL_HOST`    | `smtp.ethereal.email`      | SMTP host for resets     |
| `EMAIL_PORT`    | `587`                      | SMTP port                |
| `EMAIL_USER`    | —                          | SMTP username            |
| `EMAIL_PASS`    | —                          | SMTP password            |
| `BASE_URL`      | `http://localhost:3000`    | Used in reset email links|

## Security Notice

> ⚠️ **CSRF protection is NOT implemented** in this demo application. This app is intended for development/demo purposes only and should not be deployed to production without adding CSRF middleware (e.g., `csurf` or `csrf-csrf`).

## Tech Stack

- **Runtime**: Node.js
- **Framework**: Express.js
- **Database**: SQLite via Sequelize ORM
- **Auth**: express-session + bcryptjs
- **Views**: EJS + express-ejs-layouts
- **CSS**: Bootstrap 5 (CDN)
- **Email**: nodemailer (Ethereal test SMTP)
- **File uploads**: multer
