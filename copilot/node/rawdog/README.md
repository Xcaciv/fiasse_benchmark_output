# Loose Notes

A multi-user note-taking platform built with Node.js, Express.js, SQLite, and Bootstrap 5.

## Prerequisites
- Node.js 18+
- npm

## Installation

```bash
# 1. Install dependencies
npm install

# 2. Copy example env file
cp .env.example .env
# (Edit .env to configure session secret, SMTP, etc.)
```

## Running

```bash
# Start the server
node app.js
# or
npm start
```

The app will be available at http://localhost:3000

## Default Admin Credentials

On first run, a default admin account is created:

- **Email:** admin@example.com
- **Password:** admin123
- **Username:** admin

> **Important:** Change the admin password after first login!

## Features

- **REQ-001:** User Registration — username, email, password
- **REQ-002:** User Login/Logout — Passport.js local strategy, cookie session
- **REQ-003:** Password Reset — token-based, logged to console (configurable via SMTP)
- **REQ-004:** Note Creation — title, content, private by default
- **REQ-005:** File Attachments — pdf, doc, docx, txt, png, jpg, jpeg (max 10MB)
- **REQ-006:** Note Editing — owner/admin only, full update
- **REQ-007:** Note Deletion — owner/admin, deletes files from disk
- **REQ-008:** Note Sharing — UUID share links, public access without auth
- **REQ-009:** Public/Private Visibility — toggle on create/edit
- **REQ-010:** Note Rating — 1–5 stars + comment, one per user per note
- **REQ-011:** Rating Management — owner sees all ratings with average
- **REQ-012:** Note Search — case-insensitive, title + content, owned + public
- **REQ-013:** Admin Dashboard — user/note counts, recent activity, user management
- **REQ-014:** User Profile Management — update username, email, password
- **REQ-015:** Top Rated Notes — public notes with ≥3 ratings, sorted by avg
- **REQ-016:** Note Ownership Reassignment — admin can reassign notes to other users

## Environment Variables

See `.env.example` for all available configuration options.

| Variable | Description | Default |
|---|---|---|
| `PORT` | HTTP port | `3000` |
| `SESSION_SECRET` | Session signing secret | (insecure default) |
| `SMTP_HOST` | SMTP server host | (none, logs to console) |
| `APP_URL` | Base URL for reset links | `http://localhost:3000` |

## Logs

Application logs (auth events, admin actions) are written to `logs/app.log`.
