ΓùÅ Read copilot_prd_enhance_q4ranb3w.sw2.txt
  Γöé ~\AppData\Local\Temp\copilot_prd_enhance_q4ranb3w.sw2.txt
  Γöö 267 lines read

ΓùÅ List directory .
  Γöö 9 files found

ΓùÅ List directory configs
  Γöö 1 file found

ΓùÅ List directory instructions
  Γöö 3 files found

ΓùÅ Read copilot-instructions.md
  Γöé instructions\copilot-instructions.md
  Γöö 174 lines read

ΓùÅ Read settings.json
  Γöé configs\settings.json
  Γöö 13 lines read

ΓùÅ Search (glob)
  Γöé "**/*.md"
  Γöö 8 files found

ΓùÅ List directory prd_broad
  Γöö 1 file found

ΓùÅ List directory prd_clear
  Γöö 1 file found

ΓùÅ Read loosenotes.prd
  Γöé prd_broad\loosenotes.prd
  Γöö 105 lines read

ΓùÅ Read loosenotes.prd
  Γöé prd_clear\loosenotes.prd
  Γöö 105 lines read

# Project Requirements Document ΓÇö Loose Notes Web Application (Security-Enhanced)

## Technical Stack

- **Framework**: ASP.NET Core MVC
- **Database**: Relational database with Entity Framework Core
- **Authentication**: ASP.NET Core Identity
- **File Storage**: Local file system or cloud storage
- **Frontend**: Razor Views with JavaScript

---

## 1. Introduction

The Loose Notes Web Application is a multi-user note-taking platform that allows users to create, manage, share, and rate notes. Users can upload attachments to notes, make notes public or private, and share them with others using generated share links.

This document outlines the functional and securability requirements of the application. Requirements have been enhanced to explicitly address trust boundaries, data handling constraints, and verifiable security behaviors using FIASSE/SSEM principles and OWASP ASVS Level 2 requirements.

---

## 2. ASVS Level Decision

**Selected Assurance Level: ASVS Level 2**

**Rationale:** The Loose Notes application is a standard production web application with authenticated users, persistent user data (including PII: email addresses, usernames), file uploads, publicly accessible content, and an administrative tier capable of modifying ownership of user-owned content. These characteristics align with Level 2: typical production web/API systems with authenticated users and business-critical behavior.

**Why Level 1 is insufficient:** Level 1 is appropriate only for low-risk internal or prototype systems. This application stores user PII, manages authentication sessions, allows unauthenticated access to private notes via share links, and includes privileged admin operations. Each of these introduces trust-boundary crossings that require defensible controls.

**Why Level 3 is not required:** The system does not handle highly regulated data (e.g., healthcare, financial transactions) and does not present a high-value target warranting formal verification or hardware-backed authenticators.

**Features requiring per-requirement level escalation beyond baseline:**

| Feature | Escalation Rationale |
|---------|----------------------|
| F-08: Note Sharing | Unauthenticated access to private note content via share tokens requires share token entropy, issuance logging, and revocation to be treated with the same rigour as authenticated session tokens. |
| F-13: Admin Dashboard | Admin functions must enforce re-authentication and privileged-action audit logging beyond standard L2 to limit blast radius of compromised admin session. |

---

## 3. Feature-ASVS Coverage Matrix

| Feature | ASVS Section | Requirement ID | Level | Coverage | PRD Change Needed |
|---------|-------------|----------------|-------|----------|-------------------|
| F-01 Registration | V2.1 Password Security | 2.1.1 | 2 | Missing | Add minimum 12-character password requirement |
| F-01 Registration | V2.1 Password Security | 2.1.2 | 2 | Missing | Allow passwords up to at least 64 characters |
| F-01 Registration | V2.1 Password Security | 2.1.7 | 2 | Missing | Check passwords against known-breach list (e.g., HaveIBeenPwned API) |
| F-01 Registration | V5.1 Input Validation | 5.1.3 | 2 | Missing | Validate email format; validate username length (3ΓÇô50 chars, alphanumeric+underscore) |
| F-01 Registration | V8.3 Sensitive Data | 8.3.4 | 2 | Missing | Classify email as PII; document storage and access expectations |
| F-01 Registration | V2.2 Anti-automation | 2.2.1 | 2 | Missing | Protect registration endpoint against automated account creation |
| F-02 Authentication | V2.2 General Auth | 2.2.1 | 2 | Missing | Rate-limit and soft-lock login after repeated failures |
| F-02 Authentication | V3.2 Session Binding | 3.2.1 | 2 | Missing | Issue new session token on authentication (prevent session fixation) |
| F-02 Authentication | V3.4 Cookie-based Sessions | 3.4.1ΓÇô3.4.3 | 2 | Missing | Enforce Secure, HttpOnly, and SameSite=Strict on auth cookies |
| F-02 Authentication | V7.2 Log Processing | 7.2.1 | 2 | Partial | Log authentication events with IP, username, outcome, and timestamp |
| F-03 Password Reset | V2.5 Credential Recovery | 2.5.6 | 2 | Partial | Specify minimum token entropy (ΓëÑ128 bits); invalidate all active sessions on reset |
| F-03 Password Reset | V2.5 Credential Recovery | 2.5.7 | 2 | Missing | Do not reveal whether email is registered (consistent response for known/unknown addresses) |
| F-03 Password Reset | V3.3 Session Logout | 3.3.1 | 2 | Missing | Invalidate all active user sessions when password reset completes |
| F-04 Note Creation | V5.1 Input Validation | 5.1.3 | 2 | Missing | Define and enforce maximum lengths for title and content fields server-side |
| F-04 Note Creation | V5.3 Output Encoding | 5.3.3 | 2 | Partial | Confirm context-aware output encoding for all rendered note fields |
| F-04 Note Creation | V4.2 Access Control | 4.2.1 | 2 | Covered | Note must be linked to authenticated user's ID (server-derived, not client-supplied) |
| F-05 File Attachment | V12.1 File Upload | 12.1.1 | 2 | Partial | Define explicit maximum file size and maximum attachment count per note |
| F-05 File Attachment | V12.2 File Integrity | 12.2.1 | 2 | Missing | Validate file content type (MIME sniffing), not just extension |
| F-05 File Attachment | V12.3 File Execution | 12.3.1 | 2 | Missing | Uploaded files must not be directly executable by the web server |
| F-05 File Attachment | V12.3 Path Traversal | 12.3.3 | 2 | Missing | Sanitize and reject filenames with path traversal sequences |
| F-05 File Attachment | V12.4 File Storage | 12.4.1 | 2 | Missing | Store files outside the web root or with explicit no-execute permissions |
| F-05 File Attachment | V12.5 File Download | 12.5.1 | 2 | Missing | Set Content-Disposition: attachment when serving file downloads |
| F-06 Note Editing | V4.2 Access Control | 4.2.1 | 2 | Partial | Re-confirm server-side ownership check; document IDOR prevention explicitly |
| F-06 Note Editing | V4.2 CSRF | 4.2.2 | 2 | Covered | Anti-forgery token required (referenced in ┬º4.6); must be validated server-side |
| F-07 Note Deletion | V4.2 Access Control | 4.2.1 | 2 | Covered | Ownership check server-side before deletion |
| F-07 Note Deletion | V11.1 Business Logic | 11.1.4 | 2 | Missing | Specify cascading deletion behaviour and whether it is atomic/transactional |
| F-08 Note Sharing | V4.3 Misc Access Control | 4.3.1 | 2 | Missing | Share token must have ΓëÑ128 bits of cryptographic entropy; generated by CSPRNG |
| F-08 Note Sharing | V7.2 Logging | 7.2.2 | 2 | Missing | Log share-link generation, regeneration, revocation, and each access (anonymous or not) |
| F-08 Note Sharing | V3.7 Re-authentication | 3.7.1 | 2 | Missing | Prevent brute-force enumeration of share tokens (rate-limit unauthenticated share link lookups) |
| F-09 Public/Private | V4.2 Access Control | 4.2.1 | 2 | Partial | Server-side enforcement of visibility must be documented as authoritative; client toggle is presentation only |
| F-09 Public/Private | V8.2 Browser Caching | 8.2.1 | 2 | Missing | Private note responses must include Cache-Control: no-store |
| F-10 Note Rating | V11.1 Business Logic | 11.1.1 | 2 | Missing | Enforce one rating per user per note at the data layer, not only in application logic |
| F-10 Note Rating | V4.2 Access Control | 4.2.1 | 2 | Missing | Specify whether note owner can rate their own note (recommend disallow) |
| F-10 Note Rating | V5.1 Input Validation | 5.1.3 | 2 | Missing | Validate rating value is integer 1ΓÇô5 server-side; validate comment length server-side |
| F-11 Rating Management | V4.2 Access Control | 4.2.1 | 2 | Missing | Rater usernames visible to owner are PII; confirm access is owner-only server-side |
| F-12 Note Search | V5.1 Input Validation | 5.1.3 | 2 | Missing | Validate/sanitize search query; define maximum query length to prevent resource exhaustion |
| F-12 Note Search | V4.2 Access Control | 4.2.1 | 2 | Partial | Search result filtering (private exclusion) must be enforced in the query, not post-fetch |
| F-12 Note Search | V11.1 Business Logic | 11.1.7 | 2 | Missing | Paginate search results; enforce maximum result set size |
| F-13 Admin Dashboard | V4.3 Admin Access Control | 4.3.2 | 2 | Partial | Admin routes must be protected by role check; document that role is server-authoritative |
| F-13 Admin Dashboard | V7.2 Logging | 7.2.2 | 2 | Missing | All admin actions (user lookups, role views) must generate audit log entries |
| F-13 Admin Dashboard | V3.7 Re-authentication | 3.7.1 | 2 | Missing | Require re-authentication before accessing admin functions after session idle period |
| F-14 Profile Management | V2.1 Password Security | 2.1.5 | 2 | Missing | Require current password confirmation before allowing password or email change |
| F-14 Profile Management | V2.2 Credential Change | 2.2.3 | 2 | Missing | Notify user via email on password change or email address update |
| F-14 Profile Management | V3.3 Session Management | 3.3.1 | 2 | Missing | Invalidate all other active sessions when user changes their password |
| F-15 Top Rated Notes | V4.2 Access Control | 4.2.1 | 2 | Missing | Query must explicitly filter to public notes only at the database layer |
| F-16 Ownership Reassign | V4.3 Admin Access Control | 4.3.2 | 2 | Missing | Verify target user exists before reassignment; log action with admin ID, old owner, new owner |
| F-16 Ownership Reassign | V7.2 Logging | 7.2.2 | 2 | Missing | Audit log entry required for every reassignment (who/what/when/from/to) |

---

## 4. Enhanced Feature Specifications

### Feature F-01: User Registration (REQ-001)

**ASVS Mapping**: V2.1.1, V2.1.2, V2.1.7, V2.2.1, V5.1.3, V8.3.4

**Updated Requirements**:
- Users can register with a unique username (3ΓÇô50 characters, alphanumeric and underscores only), a valid email address, and a password.
- Passwords must be at least 12 characters long; passwords up to at least 64 characters must be accepted without truncation.
- Passwords must be checked against a known-breach list (e.g., HIBP API) at registration; if the password appears in breach data the user must choose a different password.
- Username and email uniqueness must be enforced at the database layer.
- The registration endpoint must be protected against automated account creation (rate limiting; optionally CAPTCHA for burst protection).
- Registration form returns generic validation messages that do not confirm whether a specific email or username is already registered.
- Email address is classified as PII; it is stored only in the Users table, is not logged in plaintext, and is not exposed in URLs.

**Acceptance Criteria**:
- Registration rejects passwords shorter than 12 characters with an appropriate message.
- Registration rejects passwords that appear in the HIBP breach corpus.
- Submitting the same username or email twice returns an error without revealing which field is taken (generic: "Registration failed").
- Automated registration attempts beyond 10 per minute per IP result in temporary throttling.

**Securability Notes**: The registration form is the primary trust-boundary entry point for new identities. Password strength and breach-checking are enforced server-side (Integrity: the client cannot weaken these constraints). Email as PII requires confidentiality expectations to be set early ΓÇö store it minimally, never log it, and never expose it in visible URLs. Rate limiting registration prevents low-cost account-farm abuse and protects downstream costs (e.g., email sending). The username uniqueness constraint must be enforced at the database level (unique index), not only in application code, to prevent race-condition duplicate registrations.

---

### Feature F-02: User Login / Authentication (REQ-002)

**ASVS Mapping**: V2.2.1, V2.2.2, V3.2.1, V3.4.1, V3.4.2, V3.4.3, V7.2.1

**Updated Requirements**:
- Users log in with username and password.
- The system must soft-lock an account after 5 consecutive failed login attempts (lock duration: 15 minutes or unlockable only via password reset).
- The authentication endpoint must be rate-limited per IP to prevent distributed credential-stuffing attacks.
- On successful authentication, a new session token must be issued (old pre-authentication token invalidated) to prevent session fixation.
- Authentication cookies must be issued with Secure, HttpOnly, and SameSite=Strict (or SameSite=Lax with CSRF tokens) attributes.
- Session lifetime must be defined: active sessions expire after a configurable idle timeout (recommended: 30 minutes); absolute maximum session lifetime must be enforced (recommended: 12 hours).
- All authentication attempts (success and failure) must be logged with: username attempted, outcome, source IP address, and UTC timestamp. Passwords must never appear in logs.
- Failed login error messages must be generic ("Invalid username or password") and must not reveal whether the username exists.

**Acceptance Criteria**:
- The 6th consecutive failed login attempt returns a lockout message and blocks further attempts for the configured duration.
- A session cookie issued after login has Secure, HttpOnly, and SameSite attributes visible in browser DevTools.
- Log record is created for every login attempt with outcome and IP.
- Attempting session fixation (reusing a pre-login session token post-login) results in rejection.

**Securability Notes**: Authentication is the outer trust boundary of the application. Session fixation and credential-stuffing are the highest-probability threats at this layer. The new-session-on-login requirement (Authenticity) ensures that a token observed before authentication cannot be promoted to an authenticated session. Cookie security attributes (Trustworthiness) prevent theft via network eavesdropping (Secure) and JavaScript injection (HttpOnly). Audit logging at this layer is non-negotiable for accountability: failed login patterns are the primary forensic signal for credential-stuffing and brute-force detection.

---

### Feature F-03: Password Reset (REQ-003)

**ASVS Mapping**: V2.5.6, V2.5.7, V3.3.1, V7.2.1

**Updated Requirements**:
- Users request a password reset by submitting their email address.
- The system returns the same response regardless of whether the email is registered (prevent user enumeration via reset flow).
- The reset token must be generated using a cryptographically secure random number generator (CSPRNG) with at least 128 bits of entropy.
- The reset link must be valid for 1 hour and must be single-use; used or expired tokens are rejected immediately.
- On successful password reset:
  - The old password is immediately invalidated.
  - All active sessions for the user are invalidated (forced logout on all devices).
  - A notification email is sent to the address confirming the password was changed.
- Reset tokens must not appear in server-side logs.
- New password must meet the same strength requirements as registration (ΓëÑ12 characters, not in breach list).

**Acceptance Criteria**:
- Requesting reset for a non-existent email returns the same UI response as for a real email.
- The reset token in the link is at least 24 URL-safe characters (Γëê144+ bits).
- Using an already-used reset link returns "token invalid or expired."
- All other active sessions for the user are terminated on successful reset.
- Notification email is received after a successful reset.

**Securability Notes**: Password reset is a high-value attack target because it allows unauthenticated credential replacement. The consistent response requirement (Trustworthiness/Confidentiality) prevents username/email enumeration. Token entropy (Trustworthiness/Authenticity) ensures the link is not guessable. Session invalidation on reset (Reliability/Integrity) is critical: without it, a compromised session survives a reset, defeating the recovery intent. Notification email creates an accountability trail for the account owner.

---

### Feature F-04: Note Creation (REQ-004)

**ASVS Mapping**: V4.2.1, V5.1.3, V5.3.3

**Updated Requirements**:
- Authenticated users can create notes with a title (required, maximum 200 characters) and content (required, maximum 50,000 characters).
- Title and content length limits are enforced server-side; client-side validation is supplementary only.
- The creator's user ID is derived from the authenticated session server-side and must never be accepted from user-supplied form input.
- Notes default to private visibility on creation.
- Creation timestamp is recorded server-side (UTC); client-supplied timestamps are ignored.
- All rendered note fields (title, content) must be output-encoded in context (HTML context in Razor views) to prevent stored XSS.
- If rich-text or HTML content is permitted in note content, it must be sanitized using an approved library (e.g., HtmlSanitizer) before storage.

**Acceptance Criteria**:
- Submitting a title exceeding 200 characters returns a validation error; the note is not created.
- Submitting a note body exceeding 50,000 characters is rejected server-side.
- A note created with `<script>alert(1)</script>` in the content does not execute JavaScript when the note is viewed.
- A forged form submission supplying a different user ID as owner results in the note being owned by the authenticated user, not the forged ID.
- New notes are private by default when retrieved from the database after creation.

**Securability Notes**: The critical Integrity requirement here is that the ownership relationship (user ID) is always server-derived ΓÇö the application must treat the session identity as authoritative and never accept a user ID from POST body or query string. Output encoding (Trustworthiness) prevents content entered by one user from attacking another user's browser. Field size limits (Resilience) prevent single-note storage exhaustion and excessively large payloads reaching downstream query processors.

---

### Feature F-05: File Attachment (REQ-005)

**ASVS Mapping**: V12.1.1, V12.1.2, V12.2.1, V12.3.1, V12.3.3, V12.4.1, V12.5.1

**Updated Requirements**:
- Users can upload files to notes; maximum file size is 10 MB per file; maximum 5 attachments per note.
- Allowed formats: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG.
- File type validation must check both the declared Content-Type header and the file's magic bytes (MIME sniffing); files that fail either check are rejected.
- Original filenames must be sanitised before storage: path separators (`/`, `\`, `..`) and null bytes are stripped; the sanitised original name is stored in database metadata only.
- Files are stored on disk using a server-generated UUID filename (not the original name); files must be stored outside the application web root or in a location with no web-server execute permission.
- The web server must not serve uploaded files directly from a script-executable directory; file downloads must be served through the application with a `Content-Disposition: attachment` header.
- Total attachment size per note is counted when a new file is uploaded; uploads that would exceed per-note or per-user quotas (to be defined by operations) are rejected.
- File upload and download actions are logged with user ID, note ID, filename (sanitised), and timestamp.

**Acceptance Criteria**:
- Uploading a file larger than 10 MB returns a 413 or validation error.
- Uploading a 7th file to a note with 5 existing attachments returns an error.
- Uploading a file with a `.jpg` extension but HTML magic bytes is rejected.
- Uploading a file named `../../etc/passwd` results in a sanitised filename in metadata; no filesystem path traversal occurs.
- Downloading an attachment from the browser results in a file save dialog (Content-Disposition: attachment header present).
- Uploaded files are not directly accessible via a predictable URL without going through the application's download endpoint.

**Securability Notes**: File upload is the highest-risk feature surface in this application. MIME-sniffing validation (Trustworthiness/Integrity) prevents attackers from disguising executable content as an allowed file type. Storing files outside the web root with a UUID name (Resilience) prevents both direct execution and path-guessing. Path traversal sanitisation (Integrity) prevents filenames from escaping the storage boundary. The Content-Disposition header (Confidentiality) prevents browsers from executing downloaded content inline. Logging upload/download events (Accountability) establishes a forensic trail for abuse scenarios.

---

### Feature F-06: Note Editing (REQ-006)

**ASVS Mapping**: V4.2.1, V4.2.2, V5.1.3, V5.3.3

**Updated Requirements**:
- Only the note owner or a user with the Admin role can edit a note; this check is performed server-side on every edit request.
- The ownership check must compare the authenticated session's user ID to the note's recorded owner ID in the database; it must not rely on a hidden form field or URL parameter.
- Title and content are subject to the same validation constraints as note creation (REQ-004): maximum lengths enforced server-side.
- Edit form submissions must include and validate an anti-forgery token.
- Visibility status (public/private) change is a sensitive operation: the server must confirm the user is the owner before accepting the visibility change; the field value is not accepted from client claims.
- Last-modified timestamp is updated server-side (UTC).

**Acceptance Criteria**:
- A request to edit a note owned by a different user (authenticated as User A, editing User B's note) returns 403 Forbidden.
- Removing the anti-forgery token from an edit form submission results in rejection (400 or 403).
- Submitting an edit with a content length exceeding 50,000 characters is rejected server-side.
- Visibility can only be changed by the note owner; admin editing does not grant visibility change on behalf of the owner.

**Securability Notes**: IDOR (Insecure Direct Object Reference) is the primary threat at the edit boundary. The ownership check must always run before any data mutation; framing it as server-side enforcement of a trust boundary (Trustworthiness/Authenticity) ensures no client-supplied claim can bypass it. Anti-forgery tokens (Integrity) prevent cross-site request forgery from silently mutating note content on behalf of a logged-in user.

---

### Feature F-07: Note Deletion (REQ-007)

**ASVS Mapping**: V4.2.1, V4.2.2, V11.1.4

**Updated Requirements**:
- Only the note owner or an Admin can delete a note; ownership/role check performed server-side before any deletion.
- Deletion is a state-changing operation and must be protected by an anti-forgery token (POST/DELETE request).
- Deletion must cascade atomically (within a database transaction) to all child records: attachments (and associated files on disk), ratings, and share links.
- Physical deletion of attachment files from disk must be attempted; if file removal fails (e.g., cloud storage error), the failure is logged but the database records are still removed; orphaned files are reconciled by a scheduled maintenance task.
- The application presents a confirmation prompt before the user's browser dispatches the delete request.

**Acceptance Criteria**:
- A request to delete a note owned by another user returns 403 Forbidden.
- Deletion of a note with 3 attachments removes all 3 attachment records and files; the operation is verified as atomic (either all succeed or none are removed).
- Deletion request without a valid anti-forgery token is rejected.
- After deletion, any outstanding share links for the note return 404 when accessed.

**Securability Notes**: The confirmation prompt mitigates accidental deletion but is not a security control ΓÇö it is a UX guardrail. The authoritative security control is the server-side ownership check (Trustworthiness). Cascading deletion within a transaction (Integrity/Reliability) prevents orphaned records that could leak visibility into deleted content (e.g., share links that resolve to nothing but still confirm a note existed). Logging deletion events (Accountability) supports forensic investigation if content is removed unexpectedly.

---

### Feature F-08: Note Sharing (REQ-008)

**ASVS Mapping**: V4.3.1, V3.7.1, V7.2.2

**Updated Requirements**:
- Note owners can generate a unique share link for a note (public or private).
- Share tokens must be generated using a CSPRNG with at least 128 bits of entropy; the token must be URL-safe and at minimum 22 characters.
- Share link access is unauthenticated; it crosses a trust boundary and grants read access to note content and attachments.
- Access via share link must be rate-limited per IP to prevent brute-force enumeration of share tokens.
- Share link access grants read-only access to note content and downloads of attachments; it must not enable rating, editing, or any write operation.
- The system must log every share-link access: token (hashed), note ID, source IP, and UTC timestamp.
- Regenerating a share link immediately and permanently invalidates the previous token.
- Revocation (deletion) of a share link must take effect immediately; subsequent requests to the old token return 404.
- Share links do not expire automatically; owners are responsible for revocation. This is documented as a known assumption (see ┬º7).

**Acceptance Criteria**:
- A newly generated share token is at least 22 URL-safe characters.
- Accessing the same share token more than 60 times per minute from a single IP triggers rate limiting.
- Using a revoked share token returns 404.
- Using a regenerated old token returns 404; the new token returns the note content.
- A share link to a private note allows viewing the note content without authentication.
- A share link does not expose the note editor or rating form.
- Share link access is recorded in the application audit log.

**Securability Notes**: Share links are tokens that grant unauthenticated access across a significant trust boundary ΓÇö they allow any internet user to read a private note. Token entropy (Trustworthiness/Authenticity) must be treated with the same rigour as session tokens to prevent enumeration. Rate limiting (Resilience) prevents offline or online brute-force. Explicit logging of share access (Accountability) is essential since these accesses are otherwise invisible in user-centric audit trails. Revocation must be immediate and definitive; the data layer must not cache or defer token invalidation.

---

### Feature F-09: Public/Private Notes (REQ-009)

**ASVS Mapping**: V4.2.1, V8.2.1

**Updated Requirements**:
- Notes have a server-persisted visibility field: `public` or `private`. Default is `private`.
- Visibility is a server-authoritative attribute; the toggle presented to the user is the interface for requesting a change, not a claim the server trusts directly.
- Public notes are accessible to any authenticated or unauthenticated user who navigates to or searches for them.
- Private notes are accessible only to the owner (authenticated) or via a valid share link; they must not appear in any search result for other users.
- HTTP responses for private note pages must include `Cache-Control: no-store, no-cache` to prevent private content from being stored in shared caches or browser history leaking to other users of the same device.
- Changing a note from public to private takes effect immediately for all subsequent requests; any public search indexes should not cache the result beyond the server response.

**Acceptance Criteria**:
- A private note URL accessed by a different authenticated user returns 403 Forbidden (not 404, to avoid confirming existence ΓÇö team must decide on consistent policy; recommend 404 for privacy).
- A private note does not appear in search results for other users.
- The HTTP response for a private note includes `Cache-Control: no-store`.
- Changing visibility from public to private takes effect on the next page load; the note no longer appears in public search results.

**Securability Notes**: Visibility enforcement is a quintessential server-side access-control requirement (Trustworthiness/Confidentiality). The client toggle must never short-circuit server enforcement ΓÇö every request to render or return a note must re-check visibility against the database. Cache-control headers (Confidentiality) are especially important in shared environments (corporate proxies, shared devices) where a stale cached private note could be served to an unauthorised viewer.

---

### Feature F-10: Note Rating (REQ-010)

**ASVS Mapping**: V4.2.1, V5.1.3, V11.1.1

**Updated Requirements**:
- Authenticated users can rate a note with an integer value of 1ΓÇô5 (inclusive); fractional or out-of-range values are rejected server-side.
- Each user may submit at most one rating per note; the unique constraint is enforced at the database layer (unique index on user_id + note_id).
- Note owners must not be permitted to rate their own notes.
- An optional comment may accompany the rating; comment maximum length is 1,000 characters, enforced server-side.
- Ratings are visible to any user who can view the note.
- Users can edit their existing rating; the edit timestamp is recorded.
- Rating operations (create, update) are logged with user ID, note ID, value, and timestamp.

**Acceptance Criteria**:
- Submitting a rating value of 0 or 6 returns a validation error; no rating record is created.
- Attempting to rate the same note a second time (by the same user) returns an error; the existing rating is not duplicated.
- Attempting to rate one's own note returns 403.
- A comment longer than 1,000 characters is rejected server-side.

**Securability Notes**: Business-logic integrity (Integrity) requires that rating constraints ΓÇö one per user per note, valid range, no self-rating ΓÇö are enforced at the data layer, not only in application logic, to survive concurrent submissions and future code changes. The unique-index constraint is a structural guarantee. Comment length limits (Resilience) prevent large-payload abuse. Logging rating mutations supports detection of coordinated rating manipulation.

---

### Feature F-11: Rating Management (REQ-011)

**ASVS Mapping**: V4.2.1

**Updated Requirements**:
- Note owners can view all ratings on their notes, including rating value, comment, rater username, and timestamp.
- Access to the full rating list (including rater usernames) is restricted to the note owner; other users see only aggregated rating data (average, count).
- The server must enforce ownership before returning the full rating list; the check must not rely on client-supplied note ownership claims.
- Average rating is calculated server-side.
- Ratings are returned sorted by timestamp descending.

**Acceptance Criteria**:
- A request from a non-owner user to the rating management endpoint for a note they do not own returns 403 or the aggregated view only.
- Rater usernames are not exposed in the public note view response body.

**Securability Notes**: Rater usernames constitute PII of the raters. Exposing them publicly alongside ratings would violate the confidentiality expectations of users who rate notes. The access-control check (Trustworthiness/Confidentiality) must restrict full rating detail to the note owner. Server-side calculation of average rating (Integrity) prevents clients from submitting pre-computed aggregates.

---

### Feature F-12: Note Search (REQ-012)

**ASVS Mapping**: V4.2.1, V5.1.3, V11.1.7

**Updated Requirements**:
- All users (authenticated and unauthenticated) can search public notes; authenticated users additionally see their own private notes in results.
- Search query must be validated server-side: maximum query length of 200 characters; queries that are empty or consist only of whitespace return an empty result set without error.
- Visibility filtering (excluding private notes belonging to other users) must be applied in the database query (WHERE clause), not by post-processing a full result set.
- Search results are paginated; maximum page size is 50 results; the server must not return unbounded result sets.
- Result excerpts (first 200 characters of content) must be output-encoded before rendering.
- Search functionality uses parameterized queries via Entity Framework; no raw SQL string interpolation with user input is permitted.

**Acceptance Criteria**:
- A search query longer than 200 characters is rejected or truncated server-side before execution.
- A search result for authenticated User A does not include private notes belonging to User B.
- An unauthenticated user's search returns only public notes.
- Search result pages contain at most 50 entries; requesting page 2 returns the next set.
- A search for `<script>alert(1)</script>` does not execute JavaScript in rendered results.

**Securability Notes**: The visibility filter must be a query predicate (Trustworthiness/Integrity), not a post-fetch filter ΓÇö otherwise the application fetches sensitive records into memory before discarding them, creating an unnecessary data exposure window and a potential mass-disclosure path if post-filtering logic contains a bug. Pagination (Resilience) prevents resource exhaustion via a single search request. Output encoding in excerpts (Trustworthiness) prevents stored XSS via note content appearing in search result snippets.

---

### Feature F-13: Admin Dashboard (REQ-013)

**ASVS Mapping**: V4.3.2, V3.7.1, V7.2.2

**Updated Requirements**:
- The admin dashboard and all admin controller actions are accessible only to users with the Admin role; role membership is evaluated server-side on every request.
- Admins must re-authenticate (supply current password) before accessing the admin dashboard after a period of session inactivity (recommended: 15 minutes idle triggers re-auth challenge).
- The dashboard displays: total user count, total note count, and a paginated recent activity log.
- The activity log covers: authentication events, admin actions, note reassignment events, user management actions, and access-control failures. Each entry includes: actor user ID, action type, target (user/note ID), source IP, and UTC timestamp.
- Admin user-search results (username, email, registration date, note count) are displayed in the dashboard; email addresses are not exposed in URLs or logged in plaintext.
- All admin actions initiated from the dashboard (user lookup, activity log access) are themselves logged as audit events.
- Admin dashboard forms include anti-forgery tokens.

**Acceptance Criteria**:
- A request to any admin route by a non-admin authenticated user returns 403.
- An unauthenticated request to any admin route returns 401/redirect to login.
- After 15 minutes of inactivity, accessing the admin dashboard prompts for password re-entry.
- An admin user-search action generates an audit log entry.
- Activity log entries are read-only from the dashboard; no editing or deletion interface is exposed.

**Securability Notes**: The admin tier represents the highest-privilege trust boundary in the application. Role enforcement must be purely server-side (Trustworthiness/Authenticity) ΓÇö it must not rely on a cookie value or hidden field. Re-authentication on idle (Trustworthiness) limits the blast radius of an unattended authenticated admin session. Self-auditing of admin actions (Accountability) is essential: an admin who misuses the dashboard must leave a traceable record. The activity log must be append-only from the application's perspective; no UI should offer log deletion or editing.

---

### Feature F-14: User Profile Management (REQ-014)

**ASVS Mapping**: V2.1.5, V2.2.3, V3.3.1, V4.2.1

**Updated Requirements**:
- Users can update their own username, email address, and password.
- Changing the password requires the current password to be supplied and verified before the new password is accepted.
- Changing the email address requires the current password to be verified; a confirmation email is sent to the new address and must be clicked before the email change takes effect.
- New passwords must meet the same strength requirements as registration (ΓëÑ12 characters, not in breach list).
- On successful password change, all other active sessions for the user are invalidated (the current session may be preserved or re-issued).
- The user is notified via email after a successful password or email change.
- Username uniqueness is enforced at the database layer; a change that conflicts with an existing username is rejected with a generic error.
- Users can only edit their own profile; attempts to modify another user's profile return 403.

**Acceptance Criteria**:
- Submitting a new password without providing the correct current password is rejected.
- After a password change, a session token from a device other than the one used for the change is invalidated.
- Email change does not take effect until the confirmation link in the new address is clicked.
- A notification email is sent to the old email address when a password change is completed.
- Attempting to change profile data of another user via direct ID manipulation returns 403.

**Securability Notes**: Profile management is a privileged self-service operation that directly affects authentication credentials. Requiring current-password confirmation before sensitive changes (Trustworthiness/Authenticity) ensures a session hijacker cannot silently update credentials. Session invalidation on password change (Resilience) closes the window between compromise detection and remediation. Email-confirmation of the new address (Trustworthiness) prevents an attacker with brief account access from diverting future password resets to an address they control.

---

### Feature F-15: Top Rated Notes (REQ-015)

**ASVS Mapping**: V4.2.1

**Updated Requirements**:
- The Top Rated page displays only public notes (visibility = `public`); the public filter is applied in the database query.
- Notes with fewer than 3 ratings are excluded from the list; this threshold is enforced in the query.
- Each entry shows: note title, author username, average rating, rating count, and a content preview (first 200 characters); the preview is output-encoded before rendering.
- The page is accessible to both authenticated and unauthenticated users.
- The page should include appropriate cache headers (e.g., `Cache-Control: public, max-age=60`) since the content is public; caching logic must ensure that private notes can never appear as a cache side-effect.
- The query must not allow injection of note visibility through URL parameters or form fields.

**Acceptance Criteria**:
- A note with visibility = `private` does not appear in the Top Rated list regardless of its rating count or average.
- A note with 2 ratings does not appear in the Top Rated list.
- Content preview containing HTML special characters is escaped in the rendered page.

**Securability Notes**: The primary risk here is a visibility leak ΓÇö if the public filter is accidentally omitted or bypassed, private notes could be exposed to all users. The filter must be a database-layer WHERE clause (Integrity), not a post-query filter. Output encoding of previews (Trustworthiness) is required since note content is user-generated and is now displayed in a high-traffic public context.

---

### Feature F-16: Note Ownership Reassignment (REQ-016)

**ASVS Mapping**: V4.3.2, V7.2.2

**Updated Requirements**:
- Only Admin users can reassign note ownership; the action requires an active admin session.
- Before reassignment, the system must verify that the target user (new owner) exists; attempting to reassign to a non-existent user ID returns an error and makes no change.
- The reassignment operation is atomic; either both the database record update and the audit log entry succeed, or neither is applied.
- Every reassignment generates an audit log entry containing: admin user ID, note ID, previous owner user ID, new owner user ID, and UTC timestamp.
- After reassignment, the new owner has full ownership permissions (edit, delete, share, manage ratings).
- The former owner loses all ownership permissions immediately after reassignment.
- The admin must supply an anti-forgery token with the reassignment form.

**Acceptance Criteria**:
- A reassignment request from a non-admin user returns 403.
- A reassignment to a non-existent user ID is rejected; the note owner is unchanged.
- The audit log contains a record of the reassignment with all required fields after a successful operation.
- Former owner cannot edit or delete the reassigned note immediately after reassignment.
- Reassignment form submission without a valid anti-forgery token is rejected.

**Securability Notes**: Ownership reassignment is a high-impact admin action that directly alters the access-control state of a note. Atomicity of the database update and audit log write (Integrity/Reliability) ensures there is never a state where the ownership changed but the audit trail is absent. The audit record (Accountability) is the primary control for detecting administrative abuse. Verifying the target user's existence (Integrity) prevents the system from entering an orphaned-note state.

---

## 5. Cross-Cutting Securability Requirements

### CC-01: Transport Security

- All application traffic must be served exclusively over HTTPS (TLS 1.2 minimum; TLS 1.3 preferred).
- HTTP requests must be redirected to HTTPS using a permanent (301) redirect.
- HTTP Strict Transport Security (HSTS) header must be set with a minimum max-age of 180 days.
- TLS certificates must be from a trusted CA and must not be expired; an automated renewal process must be in place.

**ASVS**: V9.1.1, V9.1.2, V9.2.1

### CC-02: HTTP Security Headers

All responses must include the following headers:

| Header | Required Value |
|--------|---------------|
| `Strict-Transport-Security` | `max-age=15552000; includeSubDomains` |
| `X-Content-Type-Options` | `nosniff` |
| `X-Frame-Options` | `DENY` or `SAMEORIGIN` |
| `Content-Security-Policy` | Restrictive policy; disallow `unsafe-inline` scripts; allow `self` by default |
| `Referrer-Policy` | `strict-origin-when-cross-origin` |
| `Permissions-Policy` | Disable unused browser features (camera, microphone, geolocation) |

Private note pages and admin pages must additionally include `Cache-Control: no-store, no-cache`.

**ASVS**: V14.4.1, V14.4.3, V14.4.4, V14.4.6

### CC-03: Anti-Forgery (CSRF) Tokens

All state-changing HTTP requests (POST, PUT, PATCH, DELETE) submitted through HTML forms must include and validate an ASP.NET Core anti-forgery token. AJAX state-changing requests must use the `RequestVerificationToken` header or equivalent.

**ASVS**: V4.2.2

### CC-04: Structured Audit Logging

- All security-relevant events must be logged in a structured, parseable format (e.g., JSON lines).
- Mandatory log fields for every security event: `timestamp` (ISO 8601 UTC), `event_type`, `actor_user_id`, `target_entity_id`, `source_ip`, `outcome` (success/failure), `session_id` (hashed).
- Security events to log (minimum set): login success, login failure, logout, password change, email change, password reset request, password reset completion, note created, note deleted, note visibility changed, file uploaded, file downloaded, share link generated/revoked/accessed, admin action (any), access control failure (403).
- Logs must never contain: passwords, raw tokens, session token values, or full email addresses in plaintext.
- Log records must be written to an append-only destination; the application must not provide an interface for modifying or deleting log records.

**ASVS**: V7.1.1, V7.1.2, V7.2.1, V7.2.2, V7.3.1

### CC-05: Error Handling

- Production error responses must not reveal stack traces, internal paths, database schema details, or framework version information to end users.
- Generic error pages (400, 403, 404, 500) must be served in all error conditions.
- Full error details (stack trace, exception type) are written to the server-side log only; they must not appear in the HTTP response body.
- Unhandled exceptions must be caught at the global filter level and logged with a correlation ID; the correlation ID may be shown to the user for support purposes.

**ASVS**: V7.4.1, V7.4.2

### CC-06: Input Validation and Output Encoding

- All input from untrusted sources (form fields, query parameters, route values, uploaded file metadata) must be validated against a positive allowlist (type, length, format).
- Razor views use `@` encoding by default; any use of `@Html.Raw()` must be explicitly justified, reviewed, and restricted to content that has been sanitized server-side.
- Search query parameters and note content previews appearing in rendered HTML must be HTML-encoded.

**ASVS**: V5.1.1, V5.1.3, V5.2.1, V5.3.3

### CC-07: Dependency and Configuration Management

- All third-party NuGet packages must be tracked with a dependency manifest; known-vulnerable packages must be updated within 30 days of a published CVE.
- Application secrets (connection strings, email API keys, encryption keys) must not be stored in source code or version control; use environment variables or a secrets manager.
- Debug and developer-exception pages must be disabled in production configuration.
- The database user account used by the application must have minimum necessary permissions (SELECT, INSERT, UPDATE, DELETE on application tables only; no schema-change permissions in production).

**ASVS**: V14.1.1, V14.2.1, V14.2.2, V14.3.3

### CC-08: Rate Limiting

- The following endpoints must have rate limits configured to prevent abuse:
  - Registration: 10 requests per IP per minute
  - Login: 20 requests per IP per minute (soft account lockout triggers at 5 per account)
  - Password reset request: 5 requests per IP per minute
  - Search: 60 requests per IP per minute
  - Share link access: 60 requests per IP per minute
  - File upload: 10 requests per authenticated user per minute
- Rate limit responses return HTTP 429 with a `Retry-After` header.

**ASVS**: V2.2.1, V11.1.7, V13.4.1

---

## 6. Updated Requirements Table

| Requirement ID | Description | User Story | Expected Behavior/Outcome |
|----------------|-------------|------------|---------------------------|
| REQ-001 | User Registration | As a visitor, I want to create an account so that I can save my notes and access them later. | Users register with a unique username (3ΓÇô50 chars, alphanumeric/underscore), a valid email address, and a password (min 12 chars, max 64 chars). Password is checked against known-breach data. Endpoint is rate-limited. Registration does not confirm whether a username or email is already taken. |
| REQ-002 | User Login/Authentication | As a registered user, I want to log in to access my notes. | Users log in with username and password. Failed attempts increment a per-account counter; account is soft-locked after 5 consecutive failures (15-minute lockout). New session token issued on success (session fixation prevention). Auth cookie is Secure, HttpOnly, SameSite=Strict. All attempts logged with IP and outcome. Error messages are generic. |
| REQ-003 | Password Reset | As a user who forgot their password, I want to reset it. | Reset request returns the same response regardless of email registration status. Reset token is CSPRNG-generated, ΓëÑ128 bits, single-use, valid 1 hour. On success: old password invalidated, all sessions invalidated, confirmation email sent. Token never appears in logs. |
| REQ-004 | Note Creation | As a logged-in user, I want to create notes with titles and content. | Authenticated users create notes with title (required, max 200 chars) and content (required, max 50,000 chars); limits enforced server-side. Owner ID is derived from session; default visibility is private; timestamp is server-side UTC. All output HTML-encoded. |
| REQ-005 | File Attachment | As a user, I want to attach files to my notes. | Max 10 MB per file, 5 files per note. Allowed types: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG. Type validated by magic bytes and Content-Type header. Filename sanitised (path traversal stripped); stored UUID on disk outside web root. Downloads served with Content-Disposition: attachment. Uploads/downloads logged. |
| REQ-006 | Note Editing | As a note owner, I want to edit my notes. | Only owner or admin can edit; server-side ownership check on every request. Anti-forgery token required. Same field-length constraints as creation. Last-modified timestamp updated server-side (UTC). |
| REQ-007 | Note Deletion | As a note owner, I want to delete my notes when I no longer need them. | Only owner or admin can delete; server-side check required. Anti-forgery token required. Deletion is atomic: note, attachments (files and records), ratings, and share links removed in a single transaction. Failed file removal is logged; DB records removed regardless. |
| REQ-008 | Note Sharing | As a note owner, I want to share my notes with specific people. | Share tokens are CSPRNG-generated, ΓëÑ128 bits, URL-safe, min 22 chars. Token grants read-only access without authentication. Access is rate-limited (60/min per IP). All access logged (hashed token, note ID, IP, timestamp). Regeneration invalidates old token immediately. No expiry (see ┬º7). |
| REQ-009 | Public/Private Notes | As a note owner, I want to control whether my notes are publicly accessible. | Visibility is server-authoritative. Private notes return Cache-Control: no-store. Visibility filter applied at query layer. Changing to private takes effect immediately. Private note access by non-owner returns 403/404. |
| REQ-010 | Note Rating | As a user, I want to rate notes to provide feedback. | Authenticated users rate 1ΓÇô5 (integer, validated server-side); one rating per user per note (enforced by DB unique index); note owner cannot rate own note. Comment max 1,000 chars. Edit allowed; mutations logged. |
| REQ-011 | Rating Management | As a note owner, I want to see ratings for my notes. | Full rating detail (value, comment, rater username, timestamp) visible to note owner only; server-side ownership check required. Other users see only average and count. Average calculated server-side. |
| REQ-012 | Note Search | As a user, I want to search for notes by keywords. | Query max 200 chars; validated server-side. Visibility filter in DB query; paginated (max 50 results/page). Parameterized queries only. Output-encoded excerpts. Unauthenticated users see public notes; authenticated see owned + public. |
| REQ-013 | Admin Dashboard | As an administrator, I want to manage users and content. | Admin role enforced server-side. Re-authentication required after 15-minute idle. Dashboard shows user count, note count, paginated activity log. Admin actions themselves are logged. Email not in URLs or plaintext logs. Anti-forgery tokens on all forms. |
| REQ-014 | User Profile Management | As a user, I want to edit my profile information. | Current password required to change password or email. New passwords must meet registration strength requirements. Email change requires confirmation via link sent to new address. Password change invalidates all other sessions. Notification email sent on password/email change. Username uniqueness enforced at DB layer. Server-side ownership check prevents editing another user's profile. |
| REQ-015 | Top Rated Notes | As a user, I want to see the highest-rated notes. | Only public notes included (filter in DB query). Minimum 3 ratings required (filter in DB query). Preview output-encoded. Public caching headers (max-age=60). Accessible to unauthenticated users. |
| REQ-016 | Note Ownership Reassignment | As an admin, I want to reassign notes to different users if needed. | Admin-only; server-side role check. Target user existence verified. DB update and audit log written atomically. Audit entry: admin ID, note ID, old owner, new owner, timestamp. Former owner loses permissions immediately. Anti-forgery token required. |

---

## 7. Open Gaps and Assumptions

| ID | Gap / Assumption | Risk | Recommended Action |
|----|------------------|------|-------------------|
| GAP-01 | Share links do not expire automatically. A revoked share link is deleted, but non-revoked links persist indefinitely. | A compromised or leaked share link remains valid until the owner manually revokes it. | Consider adding an optional expiry date field to share links, or a site-wide maximum share link lifetime. At minimum, document the no-expiry behaviour to users and provide a prominent revoke button. |
| GAP-02 | No email verification at registration. Users can register with an unverified email address. | Password reset tokens are sent to potentially unverified addresses; an attacker who registers with another person's email could disrupt that person's ability to register. | Add email verification on registration (send confirmation link; account limited to read-only until verified). |
| GAP-03 | No defined per-user storage quota for attachments. | A single user could exhaust disk storage by uploading many notes with 5 ├ù 10 MB attachments. | Define a per-user total attachment storage quota (e.g., 500 MB). Enforce at upload time. |
| GAP-04 | Content type of note content field is not specified. If the application allows HTML/Markdown rendering, an HTML sanitiser is required. If only plain text is allowed, this is simpler. | Stored XSS risk if HTML is rendered without sanitisation. | Explicitly decide: plain text only (encode everything on render) or rich text (sanitise with HtmlSanitizer before storage or on render). Document the decision in the PRD. |
| GAP-05 | No multi-factor authentication (MFA) requirement. | Account takeover risk is higher without MFA, especially for admin accounts. | Consider requiring TOTP or email OTP for admin accounts as a minimum. MFA for all users is optional at L2 but significantly reduces account takeover risk. |
| GAP-06 | Admin role assignment mechanism is not specified. The PRD does not describe how a user is promoted to the Admin role or how the initial admin account is created. | Misconfiguration of the seeding/promotion process could create unintended admin accounts. | Define the admin promotion process (e.g., seeded via configuration at startup; promoted via a CLI tool; promoted by another admin). Document the process and secure it. |
| GAP-07 | Share link access logging uses hashed token. The log will record that a share link was accessed but cannot directly map to the note without an additional lookup. | Forensic investigation requires correlating hashed token to note ID. | Store the hashed token AND the note ID in the log entry to support investigation without storing the raw token. |
| GAP-08 | The "recent activity log" in the Admin Dashboard has no defined retention period or archival strategy. | Logs could grow unboundedly or be inadvertently lost. | Define log retention period (e.g., 90 days in active storage; archived beyond that) and implement a rotation/archival policy. |
| GAP-09 | No Content Security Policy nonce or hash strategy specified for inline scripts. If Razor views use any inline `<script>` blocks, CSP `script-src` will need a nonce or hash to avoid `unsafe-inline`. | XSS mitigation via CSP is weakened if `unsafe-inline` is required. | Audit all Razor views for inline scripts; replace with external JS files or use CSP nonces. |

---

## 8. Feature Requirements by User Role (Updated)

### Regular Users
- Register (with strength-validated, breach-checked password) and log in (with lockout protection)
- Create, edit, view, and delete personal notes (server-side ownership enforced on all mutations)
- Attach files to notes (MIME-validated, size-limited, stored outside web root)
- Search for notes (owned and public; paginated; parameterized)
- Share notes with others via cryptographically strong share links (revocable; rate-limited access)
- Rate and comment on notes (one rating per user per note; no self-rating; integer 1ΓÇô5)
- Mark notes as public or private (server-side authoritative; private notes cache-controlled)
- View and edit own profile (current password required for sensitive changes; session invalidation on password change)
- Reset password (enumeration-safe; all sessions invalidated on reset)

### Admin Users
- All regular user functionalities
- Access admin dashboard (re-authentication after idle; role enforced server-side)
- View all users and their information (email not in URLs or logs)
- Reassign note ownership between users (atomic; full audit log)
- View paginated activity logs (read-only; append-only log destination)

---

## 9. Technical Requirements (Updated)

### 9.1 Architecture
- Application uses ASP.NET Core MVC pattern
- Controllers handle HTTP requests and return views or redirect results
- Models represent database entities using Entity Framework Core
- Views use Razor syntax for server-side rendering
- All state-changing requests must go through POST/PUT/DELETE with anti-forgery validation; GET requests must not cause state changes

### 9.2 Database
- Use Entity Framework Core with relational database (SQL Server, PostgreSQL, or SQLite)
- Required tables: Users, Notes, Attachments, Ratings, ShareLinks, AuditLog
- Foreign key relationships with referential integrity; cascading deletes configured for child records of Notes
- Indexes on frequently queried fields (username, note_user_id, search fields)
- Unique index on (user_id, note_id) in Ratings table
- Unique index on token column in ShareLinks table
- Database user account has minimum-privilege access (DML only; no DDL in production)

### 9.3 Authentication & Authorization
- ASP.NET Core Identity for user management with PBKDF2 password hashing (iteration count at or above ASVS 2.4.x guidance)
- Cookie-based authentication; cookies must be Secure, HttpOnly, SameSite=Strict
- New session issued on authentication (session fixation prevention)
- Role-based authorization: User (default) and Admin
- Controller actions protected with `[Authorize]` and `[Authorize(Roles = "Admin")]` attributes
- Server-side ownership checks in service layer before any mutation operation
- Account lockout after 5 consecutive failed login attempts (15-minute soft lock)
- Re-authentication challenge for admin routes after 15-minute idle

### 9.4 File Handling
- Store uploaded files with UUID-generated filenames outside the web root
- Validate both file extension (allowlist) and magic bytes (MIME type) before accepting
- Strip path traversal sequences from original filename before storing in metadata
- Serve files via application controller with Content-Disposition: attachment header
- Enforce per-file size limit (10 MB) and per-note attachment count limit (5)

### 9.5 User Interface
- Responsive design (desktop, tablet, mobile)
- Bootstrap or similar CSS framework
- Forms include client-side and server-side validation messages
- Navigation shows role-appropriate options
- Anti-forgery token meta tag or hidden field included on all state-changing forms

### 9.6 Security Controls
- Password hashing: ASP.NET Core Identity (PBKDF2); do not implement custom hashing
- Parameterized queries via Entity Framework only; no raw SQL with string interpolation
- Razor `@` encoding by default; `@Html.Raw()` restricted to pre-sanitized content only
- Anti-forgery tokens validated on all state-changing requests
- Server-side ownership/role verification before all operations (edit, delete, rate, view private, admin actions)
- Breach-password check at registration and password change
- Rate limiting on authentication, registration, search, share link access, and file upload endpoints
- HTTP security headers on all responses (see CC-02)
- All application secrets in environment variables or secrets manager; not in source control

### 9.7 Logging
- Structured JSON logging to an append-only destination
- Mandatory fields per event: timestamp (UTC ISO 8601), event_type, actor_user_id, target_entity_id, source_ip, outcome, session_id (hashed)
- Log all events defined in CC-04
- Never log: passwords, raw tokens, session token values, full email addresses
- Log retention: minimum 90 days active; archival policy to be defined by operations
- Application error logs include correlation ID; correlation ID may be surfaced to users in error pages

### 9.8 Error Handling
- Global exception filter catches unhandled exceptions; logs full detail server-side with correlation ID
- Generic error pages (400, 403, 404, 500) shown to users; no stack traces, paths, or framework info in responses
- Developer exception page disabled in production configuration

---

## 10. Conclusion

The Loose Notes Web Application provides a platform for creating, sharing, and rating notes. This enhanced document extends the functional requirements with explicit securability requirements at OWASP ASVS Level 2, incorporating FIASSE/SSEM principles across all features. Each feature includes testable acceptance criteria for its security behaviors, cross-cutting controls are specified as first-class requirements, and open gaps are documented for product and engineering decision-making before implementation begins.

cmd.exe : 
At G:\securable-fiasse_benchmark\fiasse_benchmark\scripts\PowerShell\run-codegen-copilot-claude-plugin.ps1:329 
char:30
+             $enhanceOutput = & cmd /c "copilot " @copilotArgs 2>&1 |
+                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
Total usage est:        1 Premium request
API time spent:         6m 31s
Total session time:     6m 36s
Total code changes:     +0 -0
Breakdown by AI model:
 claude-sonnet-4.6        250.4k in, 20.1k out, 177.2k cached (Est. 1 Premium request)
