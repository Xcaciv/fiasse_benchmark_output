# Loose Notes Web Application

A multi-user note-taking platform that allows users to create, manage, share, and rate notes.

## Features

- User registration and authentication
- Create, edit, and delete notes
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Public/Private note visibility
- Note sharing via unique links
- 1-5 star ratings with comments
- Note search by keywords
- Admin dashboard
- User profile management
- Password reset functionality
- Top rated notes page

## Tech Stack

- **Runtime**: Node.js
- **Framework**: Express.js
- **Database**: SQLite with better-sqlite3
- **Authentication**: Session-based with bcrypt
- **Template Engine**: EJS

## Prerequisites

- Node.js 18+
- npm

## Installation

1. Install dependencies:
```bash
npm install
```

2. Initialize the database:
```bash
npm run init-db
```

3. (Optional) Seed the database with sample data:
```bash
npm run seed
```

## Running the Application

Start the development server:
```bash
npm run dev
```

The application will be available at `http://localhost:3000`

## Default Admin Account

After seeding, you can log in with:
- Username: `admin`
- Password: `Admin123!`

## Regular User Accounts (after seeding)

- Username: `johndoe`
- Password: `Password123!`

- Username: `janedoe`
- Password: `Password123!`

## Project Structure

```
loose-notes-app/
├── src/
│   ├── server.js           # Main server file
│   ├── db/
│   │   └── database.js    # Database setup and connection
│   ├── middleware/
│   │   └── auth.js        # Authentication middleware
│   ├── routes/
│   │   ├── auth.js        # Authentication routes
│   │   ├── notes.js       # Notes CRUD routes
│   │   ├── search.js      # Search routes
│   │   └── admin.js       # Admin routes
│   ├── views/
│   │   ├── layout.ejs     # Main layout template
│   │   ├── home.ejs       # Home page
│   │   ├── auth/          # Auth-related views
│   │   ├── notes/         # Notes-related views
│   │   ├── admin/         # Admin views
│   │   └── partials/      # Reusable view components
│   └── public/
│       └── css/
│           └── style.css  # Custom styles
├── uploads/               # Uploaded files directory
├── data/                  # SQLite database
├── package.json
└── README.md
```

## Requirements

| ID | Description |
|----|-------------|
| REQ-001 | User Registration |
| REQ-002 | User Login/Authentication |
| REQ-003 | Password Reset |
| REQ-004 | Note Creation |
| REQ-005 | File Attachment |
| REQ-006 | Note Editing |
| REQ-007 | Note Deletion |
| REQ-008 | Note Sharing |
| REQ-009 | Public/Private Notes |
| REQ-010 | Note Rating |
| REQ-011 | Rating Management |
| REQ-012 | Note Search |
| REQ-013 | Admin Dashboard |
| REQ-014 | User Profile Management |
| REQ-015 | Top Rated Notes |
| REQ-016 | Note Ownership Reassignment |

## License

MIT
