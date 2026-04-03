# LooseNotes Information Exchange Platform

A multi-user web platform for creating, managing, sharing, and rating text notes.

## Tech Stack

- **React 18** + **TypeScript**
- **Vite** (build tool)
- **Tailwind CSS** (styling)
- **Recharts** (admin dashboard charts)
- **React Router v6** (client-side routing)
- **JSZip** (ZIP export/import)
- **fast-xml-parser** (XML processing)
- **crypto-js** (AES encryption utilities)

## Setup

### Prerequisites

- Node.js 18+
- npm 9+

### Install & Run

```bash
# Install dependencies
npm install

# Start development server
npm run dev
```

Open [http://localhost:5173](http://localhost:5173) in your browser.

### Build for Production

```bash
npm run build
```

The built output lands in `dist/`.

### Preview Production Build

```bash
npm run preview
```

## Deploy to Vercel

```bash
# Install Vercel CLI
npm i -g vercel

# Deploy
vercel
```

The `vercel.json` includes SPA rewrite rules so all routes resolve correctly.

## Seed Accounts

The following accounts are pre-seeded at startup:

| Username | Email                    | Password  | Role  |
|----------|--------------------------|-----------|-------|
| admin    | admin@loosenotes.com     | admin123  | admin |
| alice    | alice@example.com        | password1 | user  |
| bob      | bob@example.com          | password2 | user  |

> Data is stored in browser `localStorage`. Clearing site data resets the application to seed state.

## Features

| Feature | Route |
|---------|-------|
| Home / Public Notes | `/` |
| Login | `/login` |
| Register | `/register` |
| Password Recovery | `/forgot-password` |
| My Notes | `/notes` |
| Create Note | `/notes/create` |
| Note Detail | `/notes/:id` |
| Edit Note | `/notes/:id/edit` |
| Delete Note | `/notes/:id/delete` |
| Shared Note (no login) | `/share/:token` |
| Search Notes | `/search` |
| Top Rated Notes | `/top-rated` |
| User Profile | `/profile` |
| Rating Management | `/ratings` |
| Export / Import | `/export-import` |
| XML Processor | `/xml` |
| Request Diagnostics | `/diagnostics` |
| Admin Dashboard | `/admin` |
| Admin Users | `/admin/users` |
| Admin Reassign Note | `/admin/reassign` |
