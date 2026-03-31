# Loose Notes

A multi-user note-taking web application built with TypeScript + React + Vite + Tailwind CSS + Recharts.

## Features

- **User Authentication** — Register, login, logout, forgot/reset password
- **Note Management** — Create, edit, view, delete notes with public/private visibility
- **File Attachments** — Attach PDF, DOC, DOCX, TXT, PNG, JPG, JPEG files to notes (metadata stored)
- **Note Sharing** — Generate unique share links for any note (no auth required to view)
- **Note Rating** — Rate notes 1–5 stars with optional comments
- **Search** — Search public and your own notes by title or content
- **Top Rated** — Browse highest-rated public notes (3+ ratings required)
- **Admin Dashboard** — User management, note reassignment, activity logs, Recharts visualizations
- **Profile Management** — Edit username, email, password

## Tech Stack

- **React 18** + **TypeScript**
- **Vite** — build tool
- **React Router v6** — client-side routing
- **Tailwind CSS** — utility-first styling
- **Recharts** — charts on admin dashboard
- **Lucide React** — icons
- **localStorage** — demo data persistence (no backend required)

## Setup & Run

### Prerequisites

- Node.js 18+ and npm

### Installation

```bash
npm install
```

### Development

```bash
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.

### Production Build

```bash
npm run build
```

The output will be in the `dist/` folder.

### Preview Production Build

```bash
npm run preview
```

## Deploy to Vercel

1. Push the repo to GitHub
2. Import the project in [Vercel](https://vercel.com)
3. Vercel auto-detects Vite — no configuration needed
4. The included `vercel.json` handles SPA routing

## Demo Credentials

The app seeds demo data on first load:

| Role  | Username | Password   |
|-------|----------|------------|
| Admin | `admin`  | `Admin123!` |
| User  | `alice`  | `User123!`  |
| User  | `bob`    | `User123!`  |

## Important Notes

- **Data is stored in `localStorage`** — it is browser-local and not shared between users or devices. This is a demo/prototype setup. A production app would require a proper backend API and database.
- **Password hashing** uses base64 encoding for demo purposes only — **not production-safe**. A real app requires a backend with bcrypt/argon2.
- **File attachments** store metadata only (filename, type, size) — actual file contents are not persisted in localStorage due to size limitations.

## Project Structure

```
src/
├── types/           # TypeScript interfaces
├── utils/
│   ├── storage.ts   # localStorage CRUD + seed data
│   ├── auth.ts      # password hashing, token generation
│   └── helpers.ts   # date formatting, file utilities
├── contexts/
│   └── AuthContext.tsx
├── components/
│   ├── layout/      # Navbar, Layout, ProtectedRoute
│   └── ui/          # Button, Input, Card, Modal, etc.
└── pages/
    ├── auth/        # Login, Register, ForgotPassword, ResetPassword
    ├── notes/       # List, Create, Detail, Edit, Search, TopRated
    ├── share/       # ShareView (public, no auth)
    ├── profile/     # Profile settings
    └── admin/       # Dashboard, Users, Reassign
```
