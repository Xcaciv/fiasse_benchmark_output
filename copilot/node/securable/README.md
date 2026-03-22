# Loose Notes

A secure multi-user note-taking platform built with Node.js/Express.js.

## Prerequisites

- Node.js 18+
- npm

## Setup

1. Clone the repository
2. `npm install`
3. `cp .env.example .env` (edit as needed)
4. `npm start`

## SSEM Coverage

| Attribute | Coverage |
|---|---|
| Analyzability | Methods ≤30 LoC, cyclomatic complexity <10, clear naming |
| Modifiability | Dependency injection, centralized security logic |
| Testability | Injectable services, factory pattern for app |
| Confidentiality | No secrets in code/logs, generic error messages |
| Accountability | Structured audit logs for auth/admin/note actions |
| Authenticity | Passport.js + bcrypt, signed sessions, CSRF |
| Availability | Rate limiting, body size limits, graceful degradation |
| Integrity | Input validation at all trust boundaries, parameterized ORM queries |
| Resilience | Error handler, no resource leaks, defensive coding |
