# LooseNotes

A multi-user note-taking platform built with ASP.NET Core MVC (.NET 8).

## Prerequisites

- .NET 8 SDK

## Setup

1. Clone/download the project
2. `cd` into the project directory
3. Run `dotnet restore`
4. Run `dotnet run` (auto-migrates and seeds on first launch)

## Default Credentials

| Role  | Email                | Password  |
|-------|----------------------|-----------|
| Admin | admin@example.com    | Admin@123 |
| User  | user1@example.com    | User@123  |

## Features

- User registration, login, password reset
- Note CRUD with public/private visibility
- File attachments (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG)
- Share links for notes (unauthenticated access via token)
- 1-5 star ratings with comments
- Note search (case-insensitive, respects visibility)
- Admin dashboard (user/note management, activity log)
- Top Rated notes page
- User profile management
