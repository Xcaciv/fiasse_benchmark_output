# Loose Notes

Loose Notes is a complete Express.js web application for creating, sharing, searching, and rating notes. It includes user accounts, password reset flow, file attachments, public/private visibility, share links, profile management, and an admin dashboard for oversight and note ownership reassignment.

## Features

- User registration, login, logout, and session-based authentication
- Password reset flow with 1-hour tokens and local development email outbox
- Create, edit, view, and delete notes with public/private visibility
- File attachments with type and size validation
- Share links for notes, including regeneration and revocation
- Search across owned notes and public notes
- Ratings with 1-5 stars and optional comments
- Top Rated page for public notes with at least 3 ratings
- User profile editing for username, email, and password
- Admin dashboard with totals, user search, activity log, and note reassignment

## Tech Stack

- Node.js
- Express.js
- EJS templates
- SQLite
- Signed cookie-based authentication sessions
- Multer for file uploads

## Getting Started

### 1. Install dependencies

```bash
npm install
```

### 2. Configure environment variables

Copy `.env.example` to `.env` and adjust values if needed.

PowerShell:

```powershell
Copy-Item .env.example .env
```

Key settings:

- `PORT`: Port to run the app on
- `SESSION_SECRET`: Session signing secret
- `BASE_URL`: Public base URL used in generated share and reset links
- `DEFAULT_ADMIN_USERNAME`, `DEFAULT_ADMIN_EMAIL`, `DEFAULT_ADMIN_PASSWORD`: Initial admin account credentials

### 3. Run the application

```bash
npm start
```

Then open `http://localhost:3000`.

## Default Admin Account

On first start, Loose Notes ensures there is at least one admin user. By default:

- Username: `admin`
- Email: `admin@example.com`
- Password: `Admin123!`

Change these values in `.env` before first launch for safer local use.

## Project Structure

```text
src/
  app.js
  server.js
  config.js
  db.js
  lib/
  middleware/
  routes/
  views/
public/
  css/
data/
uploads/
```

## Notes About Password Reset Email

For local development, password reset messages are written as JSON files into `data/outbox/` instead of being sent through SMTP. Each file contains the reset link and message content so the flow can be tested end to end without external services.

## File Upload Rules

- Allowed formats: `PDF`, `DOC`, `DOCX`, `TXT`, `PNG`, `JPG`, `JPEG`
- Maximum size per file: `10 MB`
- Uploaded files are stored in the local `uploads/` directory with generated filenames

## Data Storage

- Application data is stored in `data/loose-notes.sqlite`
- Uploaded files are stored in `uploads/`

## Security Notes

- Passwords are hashed with bcrypt
- Session cookies are HTTP-only
- State-changing forms require a CSRF token
- Access checks protect private notes, editing, deletion, downloads, and admin features

## Running in Development

```bash
npm run dev
```

## Cleanup

To reset local data, stop the app and delete:

- `data/loose-notes.sqlite`
- files inside `data/outbox/`
- uploaded files inside `uploads/` except `.gitkeep`
