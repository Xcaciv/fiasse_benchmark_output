# Loose Notes

A multi-user note-taking web application built with Node.js and Express.js. Users can create, manage, share, and rate notes with file attachments.

## Features

- **User authentication** — Register, login, logout, password reset
- **Notes** — Create, edit, delete with public/private visibility control
- **File attachments** — Upload PDF, DOC, DOCX, TXT, PNG, JPG, JPEG files
- **Sharing** — Generate unique share links for any note (no auth required to view)
- **Ratings** — 1–5 star ratings with optional comments on any note
- **Search** — Full-text search across titles and content
- **Top Rated** — Leaderboard of highest-rated public notes (min. 3 ratings)
- **Admin dashboard** — User management, activity log, note ownership reassignment
- **Profile management** — Update username, email, and password

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Framework | Express.js |
| Database | SQLite (via better-sqlite3) |
| Auth | Passport.js (local strategy) + express-session |
| Templates | EJS |
| CSS | Bootstrap 5 |
| File uploads | Multer |
| Passwords | bcryptjs (PBKDF2-like) |

## Requirements

- Node.js 18 or higher
- npm

## Setup

1. **Clone / navigate to the project directory:**

   ```bash
   cd loose-notes
   ```

2. **Install dependencies:**

   ```bash
   npm install
   ```

3. **Configure environment (optional):**

   ```bash
   cp .env.example .env
   # Edit .env to set SESSION_SECRET and PORT
   ```

4. **Start the application:**

   ```bash
   npm start
   # or for development with auto-reload:
   npm run dev
   ```

5. **Open your browser:**

   ```
   http://localhost:3000
   ```

## Default Admin Account

On first run a default admin account is created automatically:

| Field | Value |
|-------|-------|
| Username | `admin` |
| Password | `admin123` |
| Email | `admin@example.com` |

**Change the admin password after first login via Edit Profile.**

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `PORT` | `3000` | HTTP port |
| `SESSION_SECRET` | `loose-notes-secret-key` | Secret for session signing |
| `DB_PATH` | `loose_notes.db` | SQLite database file path |

## Password Reset

Email is not configured by default. When a password reset is requested:

1. Submit the forgot-password form with a registered email address.
2. The reset URL is printed to the **server console** (stdout).
3. Copy the URL from the console and open it in a browser.

To enable real email, add SMTP support via [nodemailer](https://nodemailer.com/) in `routes/auth.js`.

## File Uploads

Uploaded files are stored in the `uploads/` directory with UUID-based filenames. Allowed formats:

- Documents: PDF, DOC, DOCX, TXT
- Images: PNG, JPG, JPEG

Maximum file size: **10 MB** per file.

## Project Structure

```
loose-notes/
├── app.js                  # Express application entry point
├── package.json
├── .env.example
├── database/
│   └── db.js               # SQLite setup and helper functions
├── middleware/
│   ├── auth.js             # Passport config, auth guards
│   └── upload.js           # Multer file upload config
├── routes/
│   ├── auth.js             # /auth/* — login, register, reset
│   ├── notes.js            # /notes/* — CRUD, sharing, rating
│   ├── admin.js            # /admin/* — dashboard, user management
│   ├── profile.js          # /profile/* — profile editing
│   └── share.js            # /share/:token — public share view
├── views/
│   ├── partials/
│   │   ├── header.ejs      # Navbar, flash messages
│   │   └── footer.ejs      # Bootstrap JS, closing tags
│   ├── index.ejs           # Landing page
│   ├── error.ejs           # Error page
│   ├── auth/               # Login, register, password reset views
│   ├── notes/              # Note CRUD, search, top-rated, share views
│   ├── admin/              # Admin dashboard, user list, reassign views
│   └── profile/            # Profile edit view
├── public/                 # Static assets (CSS, JS)
└── uploads/                # Uploaded files (auto-created)
```

## Admin Features

Access the admin panel at `/admin/dashboard` (admin role required):

- **Dashboard** — User/note counts and recent activity log
- **Manage Users** — List and search all users with note counts
- **Reassign Notes** — Transfer note ownership between users

## License

MIT
