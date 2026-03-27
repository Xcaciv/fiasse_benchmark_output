ΓùÅ Read copilot_prd_enhance_npf2ixju.5kf.txt
  Γöé ~\AppData\Local\Temp\copilot_prd_enhance_npf2ixju.5kf.txt
  Γöö 267 lines read

ΓùÅ List directory .
  Γöö 9 files found

ΓùÅ List directory instructions
  Γöö 3 files found

ΓùÅ List directory configs
  Γöö 1 file found

ΓùÅ Read copilot-instructions.md
  Γöé instructions\copilot-instructions.md
  Γöö 174 lines read

ΓùÅ Read settings.json
  Γöé configs\settings.json
  Γöö 13 lines read

ΓùÅ Search (glob)
  Γöé "**/*.md"
  Γöö 8 files found

ΓùÅ List directory prd_clear
  Γöö 1 file found

ΓùÅ List directory prd_broad
  Γöö 1 file found

ΓùÅ Read README.md
  Γöö 5 lines read

ΓùÅ Read loosenotes.prd
  Γöé prd_broad\loosenotes.prd
  Γöö 105 lines read

ΓùÅ Read loosenotes.prd
  Γöé prd_clear\loosenotes.prd
  Γöö 105 lines read

Now I have everything I need. Let me produce the enhanced PRD following the play procedure.

# Project Requirements Document - Loose Notes Web Application (Security-Enhanced)

## Technical Stack

- **Framework**: ASP.NET Core MVC
- **Database**: Relational database with Entity Framework Core
- **Authentication**: ASP.NET Core Identity
- **File Storage**: Local file system or cloud storage
- **Frontend**: Razor Views with JavaScript

---

## ASVS Level Decision

**Selected Level: ASVS Level 2**

The Loose Notes application is a standard production web application with authenticated users, role-based access control (including privileged admin operations), unauthenticated share-link access to content, file upload capability, and credential management workflows (registration, login, password reset). These characteristics require verified, defense-in-depth controls at Level 2.

Level 1 is insufficient because the application handles user credentials, session state, file uploads, and admin-level operations that collectively raise the risk profile above a low-risk prototype. Level 3 is not warranted at this time because the system does not handle regulated financial data, healthcare records, or other high-sensitivity classifications requiring formal verification.

**Features requiring attention beyond baseline Level 2:**
- REQ-005 (File Attachment): elevated attention on V12 File Upload due to malicious content risk
- REQ-008 (Note Sharing): unauthenticated access path requires explicit scoping and token entropy controls
- REQ-013/REQ-016 (Admin operations): admin interfaces should enforce re-authentication or elevated session assurance

---

## Feature-ASVS Coverage Matrix

| Feature | ASVS Section | Requirement ID | Level | Coverage | PRD Change Needed |
|---------|-------------|----------------|-------|----------|-------------------|
| REQ-001 | V2.1 Password Security | 2.1.1 | 2 | Missing | Add minimum password length ΓëÑ 12 characters (or ΓëÑ 8 with compensating controls) |
| REQ-001 | V2.1 Password Security | 2.1.7 | 2 | Missing | Check passwords against known-breached password list on registration |
| REQ-001 | V2.1 Password Security | 2.1.12 | 2 | Missing | Prohibit password hints and knowledge-based security questions |
| REQ-002 | V2.2 Authenticator Security | 2.2.1 | 2 | Missing | Add brute-force protection: account lockout or progressive delay after N failed attempts |
| REQ-002 | V3.2 Session Binding | 3.2.1 | 2 | Missing | Issue new session token on successful authentication (session fixation prevention) |
| REQ-002 | V3.2 Session Binding | 3.2.3 | 2 | Missing | Session cookies must set HttpOnly, Secure, and SameSite=Strict attributes |
| REQ-002 | V3.3 Session Logout | 3.3.1 | 2 | Missing | Logout must fully invalidate the server-side session |
| REQ-003 | V2.5 Credential Recovery | 2.5.4 | 2 | Missing | Reset link must not transmit new cleartext password; user sets new password after following link |
| REQ-003 | V2.5 Credential Recovery | 2.5.7 | 2 | Missing | Rate-limit password reset requests per email/IP to prevent abuse |
| REQ-004 | V5.1 Input Validation | 5.1.1 | 2 | Missing | Server-side validation: title max 200 chars, content max 50,000 chars, both required |
| REQ-005 | V12.1 File Upload | 12.1.1 | 2 | Partial | Enforce max file size (10 MB) and max files per note (5) in server-side validation |
| REQ-005 | V12.1 File Upload | 12.1.3 | 2 | Missing | Validate MIME type via magic-byte inspection, not only file extension |
| REQ-005 | V12.3 File Execution | 12.3.1 | 2 | Missing | Reject path traversal characters in filenames; do not use original filename for storage path |
| REQ-005 | V12.4 File Storage | 12.4.1 | 2 | Missing | Store uploaded files outside the web-accessible root or behind authenticated serving endpoint |
| REQ-006 | V4.1 Access Control | 4.1.1 | 2 | Covered | Ownership verified server-side before edit |
| REQ-007 | V4.2 IDOR | 4.2.2 | 2 | Covered | Owner or admin check enforced before deletion |
| REQ-007 | V7.2 Log Events | 7.2.1 | 2 | Missing | Log note deletion events: user ID, note ID, timestamp |
| REQ-008 | V3.1 Session Tokens | 3.1.1 | 2 | Missing | Share link tokens must have ΓëÑ 128 bits of entropy (minimum 32 cryptographically random characters) |
| REQ-008 | V4.3 Access Control | 4.3.2 | 2 | Missing | Unauthenticated share access must be scoped to note content only; must not expose other user data or listings |
| REQ-008 | V7.2 Log Events | 7.2.2 | 2 | Missing | Log share link access events: link token hash, note ID, requester IP, timestamp |
| REQ-009 | V4.1 Access Control | 4.1.1 | 2 | Partial | Add explicit requirement: direct URL access to a private note is blocked for non-owners |
| REQ-010 | V5.1 Input Validation | 5.1.1 | 2 | Missing | Validate rating is integer 1ΓÇô5 server-side; enforce max comment length (1,000 characters) |
| REQ-010 | V4.2 IDOR | 4.2.2 | 2 | Missing | Enforce server-side: one rating per user per note; edit only permitted on own rating |
| REQ-012 | V5.1 Input Validation | 5.1.3 | 2 | Missing | Enforce search query length limit server-side; validate and sanitize input before query execution |
| REQ-012 | V4.1 Access Control | 4.1.2 | 2 | Partial | Confirm search results never return private notes from other users, including by crafted queries |
| REQ-013 | V4.3 Admin UI | 4.3.1 | 2 | Missing | Admin dashboard access should require re-authentication or elevated session confirmation |
| REQ-013 | V7.2 Log Events | 7.2.2 | 2 | Missing | Log all admin dashboard access events with admin user ID and timestamp |
| REQ-014 | V2.1 Password Security | 2.1.6 | 2 | Missing | Password change requires verification of current password |
| REQ-014 | V2.5 Credential Recovery | 2.5.6 | 2 | Missing | Email address changes should require current-password confirmation and trigger verification to the new address |
| REQ-016 | V7.2 Log Events | 7.2.1 | 2 | Partial | Log ownership reassignment: admin user ID, note ID, old owner ID, new owner ID, timestamp |

---

## Enhanced Feature Specifications

### Feature REQ-001: User Registration

**ASVS Mapping**: V2.1.1, V2.1.7, V2.1.12, V2.4.1

**Updated Requirements**:
- Users can register with a username (3ΓÇô50 characters, unique), email address (valid format), and password.
- Password must be at least 12 characters. No maximum length below 64 characters. Unicode passphrase characters must be permitted.
- Passwords must be checked against a known-breached password list (e.g., HIBP k-anonymity API or equivalent offline list) at registration and rejected if found.
- Password hints and knowledge-based security question prompts are not permitted.
- Passwords are stored using ASP.NET Core Identity's PBKDF2-based hashing with per-user salt.
- Registration form includes anti-forgery token. All validation is enforced server-side in addition to client-side hints.

**Securability Notes**: The registration boundary is the first trust boundary crossed by an untrusted actor. Password policy must be enforced at the server, not just in form hints, to prevent bypass via direct HTTP requests. Breached-password checking addresses the most common credential-stuffing vector without imposing undue composition restrictions. Storing a password hint would re-introduce a confidentiality leak. The Identity framework's hashing is adequate, but teams must verify the iteration count remains current with recommended PBKDF2 settings (ΓëÑ 600,000 iterations for SHA-256 as of 2023 guidance). Registration events should be observable: log new account creation (username, timestamp, source IP) to support account-abuse investigations.

**Acceptance Criteria**:
- Registration rejects passwords shorter than 12 characters with a specific error message.
- Registration rejects passwords found in the configured breached-password list with a descriptive message.
- No password hint field exists in the registration form or database schema.
- A successful registration event is written to the application log with user ID and timestamp (no password or email logged).
- Submitting the registration form without the anti-forgery token returns HTTP 400.

---

### Feature REQ-002: User Login/Authentication

**ASVS Mapping**: V2.2.1, V3.2.1, V3.2.3, V3.3.1, V7.2.1

**Updated Requirements**:
- Users authenticate with username and password.
- After 5 consecutive failed login attempts for a given account, the account is temporarily locked for a minimum of 15 minutes, or progressive delay is applied (implementation team to choose approach consistent with V2.2.1).
- Successful authentication must issue a new session token (invalidating any pre-authentication session) to prevent session fixation.
- Authentication cookies must be configured with HttpOnly, Secure (HTTPS only), and SameSite=Strict attributes.
- Logout must invalidate the server-side session record, not merely clear the client-side cookie.
- Failed login attempts return a generic error message ("Invalid username or password") without distinguishing which field was incorrect.
- Authentication events (login success, login failure, lockout, logout) are logged with user identifier, timestamp, and source IP.

**Securability Notes**: Session management is the core trust enforcement mechanism for all post-authentication features. Issuing a fresh session token on login removes a well-known fixation vector. Cookie attributes limit cross-site request and exfiltration risks. Lockout or progressive delay meaningfully raises the cost of credential-stuffing. The generic error message prevents username enumeration. Accountability requires that login events are logged with sufficient fidelity to reconstruct an incident timeline. Logout invalidating the server-side session prevents token replay if a cookie is captured after logout.

**Acceptance Criteria**:
- 5 consecutive failed logins for the same account triggers lockout; subsequent attempts return a locked-account message.
- A new session cookie is issued on every successful authentication.
- Authentication cookies include HttpOnly, Secure, and SameSite=Strict attributes (verifiable via browser dev tools or integration test).
- Posting a logout request followed by reusing the prior session cookie returns HTTP 401 or redirects to login.
- All login success and failure events appear in the application log within 1 second of the event.

---

### Feature REQ-003: Password Reset

**ASVS Mapping**: V2.5.3, V2.5.4, V2.5.5, V2.5.7

**Updated Requirements**:
- Users request a password reset by submitting their email address.
- The system sends a reset email containing a one-time token; the email body must never contain a new password in cleartext.
- Reset tokens must expire after 1 hour and be single-use; used or expired tokens are rejected with a clear error.
- Password reset requests are rate-limited: maximum 3 reset emails per email address per hour and maximum 10 requests per IP per hour.
- The response to a reset request must be identical whether or not the email address exists in the system (prevent email enumeration).
- New password submitted via the reset form must satisfy the same strength requirements as registration (ΓëÑ 12 chars, not breached).
- Password reset events are logged: user ID (if resolved), timestamp, source IP.

**Securability Notes**: The password reset flow is an alternative authentication path and therefore a high-value target. Token expiry and single-use invalidation limit the window for link interception. The indistinguishable response prevents account enumeration via the reset form. Rate limiting prevents the reset endpoint from being used to flood email inboxes or enumerate valid addresses at scale. Cleartext password transmission is explicitly prohibited because it would compromise confidentiality even over HTTPS if email is intercepted. The reset token itself is a credential and must be generated with cryptographic randomness (ΓëÑ 128 bits).

**Acceptance Criteria**:
- Requesting a reset for a non-existent email returns the same HTTP 200 and same UI message as a valid email.
- A reset link used once cannot be reused; the second attempt returns an error.
- A reset link not used within 1 hour returns an expired-token error.
- More than 3 reset requests for the same email within 1 hour result in rate-limit rejection (HTTP 429 or equivalent).
- The reset email contains a link but no new or current password in the body.
- Reset event is written to application log with timestamp and source IP.

---

### Feature REQ-004: Note Creation

**ASVS Mapping**: V5.1.1, V5.3.1, V4.1.1, V7.2.1

**Updated Requirements**:
- Authenticated users can create notes with title (required, maximum 200 characters) and content (required, maximum 50,000 characters).
- Title and content length limits are enforced server-side; requests exceeding limits return HTTP 400 with a descriptive validation message.
- All input is validated server-side regardless of client-side validation.
- Each note is linked to the authenticated creator's user ID (server-assigned; not accepted from the request body).
- Notes default to private visibility on creation.
- Creation timestamp is server-assigned.
- Note creation events are logged: user ID, note ID, visibility setting, timestamp.

**Securability Notes**: Note title and content cross a trust boundary from the client; both must be validated and stored safely. The owner association must be derived from the authenticated session identity, not from any client-supplied parameter (Derived Integrity principle). Output encoding in Razor views prevents stored XSS when note content is rendered. Server-side length enforcement limits both denial-of-service via oversized payloads and database column overflow risks. Log entries at creation give the accountability baseline for the note lifecycle.

**Acceptance Criteria**:
- Submitting a note with a title > 200 chars or content > 50,000 chars returns HTTP 400 with a validation message.
- The note's owner ID is always set to the session user's ID; supplying a different user ID in the POST body is silently ignored.
- Note creation is logged with note ID and user ID.
- Rendered note titles and content in views are HTML-encoded (no raw HTML injection).

---

### Feature REQ-005: File Attachment

**ASVS Mapping**: V12.1.1, V12.1.3, V12.3.1, V12.3.2, V12.4.1

**Updated Requirements**:
- Users can attach up to 5 files per note; each file must not exceed 10 MB. Both limits are enforced server-side.
- Permitted file formats: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG. Validation must check file content (magic bytes / MIME sniffing), not only the file extension.
- Files are stored with server-generated unique identifiers; the original filename must never be used as the storage path or included in any server-side file system operation.
- Original filenames stored in database metadata must be sanitized (strip path traversal characters, control characters) before storage.
- Uploaded files must be stored outside the web-accessible root or served only through an authenticated/authorized serving endpoint that enforces access control before streaming the file.
- Direct execution or server-side rendering of uploaded file content is prohibited.
- File upload events are logged: user ID, note ID, generated file identifier, original filename hash, file size, timestamp.

**Securability Notes**: File upload is the highest-risk surface in this application. Path traversal prevention and separation of storage path from user-supplied filename are non-negotiable: storing a file at a path derived from user input enables overwrite and directory traversal attacks. Magic-byte validation prevents extension-spoofing (e.g., a PHP script renamed to `.jpg`). Serving files through an authenticated endpoint enforces the visibility model (private-note attachments should not be accessible via a guessable URL). Logging original filename hashes (not plaintext, to avoid logging PII filenames) plus the generated ID gives an investigation trail without sensitive data leakage.

**Acceptance Criteria**:
- Uploading a file larger than 10 MB returns HTTP 400.
- Uploading a file with a disallowed extension returns HTTP 400.
- Uploading a file with an allowed extension but a MIME type mismatch (e.g., a ZIP with `.jpg` extension) returns HTTP 400.
- A filename containing `../` or `..\\` is sanitized; the stored metadata does not contain traversal sequences.
- Accessing an attachment URL directly without authentication (or with a different authenticated user for a private note) returns HTTP 401/403.
- Files are not stored in `wwwroot` or any web-accessible directory.

---

### Feature REQ-006: Note Editing

**ASVS Mapping**: V4.1.1, V4.2.2, V5.1.1

**Updated Requirements**:
- Note owners can modify title, content, and visibility status of their own notes.
- Admins can edit any note.
- Authorization check (ownership or admin role) is enforced server-side on every edit request; the note ID in the request is verified against the session user.
- Title and content length limits (200 / 50,000 characters) are enforced on edit, same as creation.
- Last modified timestamp is server-assigned on save; client-supplied timestamps are ignored.
- Note edit events are logged: user ID, note ID, fields changed (not content values), timestamp.

**Securability Notes**: Edit operations are a common target for IDOR: an attacker changes the note ID in the form POST to modify another user's note. The server must resolve the note ID to a database record and verify ownership before applying any changeΓÇönot rely on the form rendering the correct note. Visibility changes (private ΓåÆ public) are particularly sensitive because they change the access boundary of existing content. Logging which fields were changed (without logging full content) maintains accountability without introducing a data-retention issue.

**Acceptance Criteria**:
- Submitting an edit request with a note ID belonging to a different user returns HTTP 403.
- Changing visibility to public for a note owned by another user via IDOR returns HTTP 403.
- Last modified timestamp reflects server time, not any client-supplied value.
- Edit events appear in the application log with user ID, note ID, and timestamp.

---

### Feature REQ-007: Note Deletion

**ASVS Mapping**: V4.1.1, V4.2.2, V7.2.1

**Updated Requirements**:
- Note owners or admins can permanently delete a note and all associated attachments, ratings, and share links.
- Ownership or admin check is enforced server-side before deletion proceeds.
- Deletion is confirmed via a confirmation prompt to prevent accidental deletion.
- Deletion is permanent and cascades to all associated data.
- Deletion events are logged: user ID, note ID, number of attachments and ratings removed, timestamp. Physical file deletion from storage must also be confirmed (or failure logged).

**Securability Notes**: Deletion cascades to attachments means physical files on disk or in cloud storage must also be removed; failure to delete physical files leaks content even after the database record is gone. Logging deletion provides the accountability anchor for audit and forensic use cases. IDOR protection (same pattern as editing) prevents attackers from deleting others' notes via guessed IDs. The confirmation prompt is a usability control that also reduces automated-script abuse.

**Acceptance Criteria**:
- Attempting to delete a note by another user returns HTTP 403.
- Deletion event is written to the application log with note ID, user ID, and cascade count.
- After deletion, the note URL returns HTTP 404.
- After deletion, direct URLs to attached files return HTTP 404 and files are removed from storage.

---

### Feature REQ-008: Note Sharing

**ASVS Mapping**: V3.1.1, V4.3.2, V7.2.2

**Updated Requirements**:
- Note owners can generate share links for any of their notes.
- Share link tokens must be generated using a cryptographically secure random source with at least 128 bits of entropy (minimum 22 Base64url characters or 32 hex characters).
- Anyone with a valid share link can view note content and attachments without authentication; this access is scoped strictly to the targeted note and its attachments.
- Share link access must not expose the owner's profile, other notes, or any listings.
- Share links can be regenerated (old token immediately invalidated) or revoked by the owner.
- Share link access events are logged: token identifier (hashed, not plaintext), note ID, requester IP, timestamp.
- Share links do not expire automatically by default, but owners may optionally set an expiration date.

**Securability Notes**: Share links create an unauthenticated access path that bypasses the normal authentication boundary. Token entropy is critical: a short or predictable token is easily brute-forced. Logging access by token hash (not the plaintext token, which is a credential) enables investigation of unauthorized access without re-exposing the credential. The scope of unauthenticated access must be strictly bounded to prevent a share link from becoming a pivot to other content. Token revocation (not just expiry) is necessary for incident response when a link is believed to have been compromised.

**Acceptance Criteria**:
- Generated share tokens are at minimum 22 Base64url characters (128-bit entropy).
- Accessing a revoked share link returns HTTP 404 or HTTP 403.
- Share link access is scoped: the response contains only the target note and its attachments, no user profile or listing data.
- Share link access events appear in the application log with hashed token and requester IP.
- Regenerating a share link immediately invalidates the previous token.

---

### Feature REQ-009: Public/Private Notes

**ASVS Mapping**: V4.1.1, V4.1.2

**Updated Requirements**:
- Notes have a visibility toggle: public or private. Default is private.
- Public notes are accessible by any user (authenticated or unauthenticated via direct URL or search).
- Private notes are accessible only to the owner; direct URL access by any other user returns HTTP 403, regardless of authentication state.
- The visibility enforcement is applied on every note view request server-side; client-side visibility state is not trusted.
- Visibility changes are logged: user ID, note ID, old visibility, new visibility, timestamp.

**Securability Notes**: The private/public boundary is a core data-confidentiality control. The most common failure mode is enforcing visibility only in search result filtering while forgetting to enforce it on the direct-access endpoint. Both the search path and the direct note view path must independently enforce visibility. Logging visibility changes supports audit of accidental public exposure of sensitive content.

**Acceptance Criteria**:
- A direct GET request to a private note URL by a different authenticated user returns HTTP 403.
- A direct GET request to a private note URL by an unauthenticated user returns HTTP 401 or HTTP 403.
- Search results never include private notes owned by other users.
- Changing visibility from private to public is logged with old and new state.

---

### Feature REQ-010: Note Rating

**ASVS Mapping**: V5.1.1, V4.2.2, V4.1.1

**Updated Requirements**:
- Authenticated users can submit a 1ΓÇô5 integer star rating (required) and an optional comment (maximum 1,000 characters).
- Rating value is validated server-side to be an integer in the range [1, 5]; out-of-range or non-integer values return HTTP 400.
- Each user may submit only one rating per note; duplicate submissions return a conflict error.
- Users may edit their own rating; editing is enforced server-side: only the rating's creator may modify it.
- Ratings on private notes are visible only to the note owner and the rating author.
- Rating submission events are logged: user ID, note ID, timestamp (no comment content logged).

**Securability Notes**: The one-rating-per-user constraint and rating-edit ownership check are IDOR-adjacent: without server-side enforcement, an attacker can submit multiple ratings inflating/deflating scores or overwrite another user's rating. Comment content crosses a trust boundary and must be output-encoded when rendered in any view. Rating values used in average calculations should be derived server-side to prevent client-supplied score manipulation.

**Acceptance Criteria**:
- Submitting a rating with value 0, 6, or a non-integer returns HTTP 400.
- Submitting a second rating for the same note by the same user returns HTTP 409 (Conflict) or equivalent.
- Attempting to edit another user's rating returns HTTP 403.
- Comment content is HTML-encoded when rendered in note views.

---

### Feature REQ-011: Rating Management

**ASVS Mapping**: V4.1.1, V4.2.2

**Updated Requirements**:
- Note owners can view a list of all ratings on their own notes, including: rating value, comment, rater username, and timestamp.
- Access to the rating management view is restricted server-side to the note owner; other users receive HTTP 403.
- Average rating is calculated server-side from stored rating values; the calculation is not influenced by any client-supplied parameter.
- Ratings are sorted by date (newest first) and paginated for notes with many ratings.

**Securability Notes**: The rating management view exposes rater usernames, which is PII-adjacent; it should be accessible only to the note owner (verified server-side). Average rating calculation must be a server-side aggregate, not a stored value that a client or attacker can directly manipulate via a crafted request.

**Acceptance Criteria**:
- Requesting the rating management view for a note owned by another user returns HTTP 403.
- Average rating displayed matches the server-side computed average from the database.
- The endpoint does not accept a client-supplied average or rating count parameter.

---

### Feature REQ-012: Note Search

**ASVS Mapping**: V5.1.1, V5.1.3, V4.1.2, V13.2.1

**Updated Requirements**:
- All authenticated users can search notes by keyword matching title or content (case-insensitive).
- Search query input is validated server-side: minimum 1 character, maximum 200 characters; requests outside this range return HTTP 400.
- Search queries are executed via parameterized queries (Entity Framework LINQ); full-text or LIKE queries must not concatenate user input into query strings.
- Search results include only: notes owned by the requesting user (any visibility) and public notes from other users. Private notes from other users are never returned regardless of query craft.
- Results show: title, excerpt (first 200 characters of content, HTML-encoded), author username, creation date.
- Search requests are rate-limited to prevent enumeration or denial-of-service.

**Securability Notes**: Search is the widest data-exposure surface in the application. The visibility filter must be applied in the database query (using a server-resolved user ID predicate), not as a post-query client-side filter, to prevent private-note leakage. Input length limits and rate limiting prevent the search endpoint from being used as a brute-force enumeration tool. Excerpts must be output-encoded to prevent stored XSS embedded in note content from executing when displayed in search results.

**Acceptance Criteria**:
- A search query longer than 200 characters returns HTTP 400.
- Search results never include private notes from other users, verified by creating a private note as User A and searching as User B.
- Excerpts in results are HTML-encoded (script tags in note content do not execute in search results).
- More than 30 search requests per minute from the same authenticated user returns HTTP 429.

---

### Feature REQ-013: Admin Dashboard

**ASVS Mapping**: V4.3.1, V4.3.2, V7.2.1, V7.2.2

**Updated Requirements**:
- Admin dashboard is accessible only to users with the Admin role; access by non-admin users returns HTTP 403.
- Admin dashboard access requires re-authentication or elevated session confirmation (e.g., re-entering password before sensitive admin operations).
- Dashboard displays: total user count, total note count, recent activity log summary.
- Admins can view all users with registration date and note count; can search users by username or email.
- All admin dashboard visits and actions are logged: admin user ID, action type, target resource ID, timestamp.
- Admin search queries on users are logged: admin user ID, query string, result count, timestamp.

**Securability Notes**: The admin interface is the highest-privilege surface and a primary target for privilege escalation attacks. Re-authentication before sensitive admin operations limits the blast radius of session hijacking. All admin actions require comprehensive logging for accountability: an audit trail of who did what to whom is essential for incident investigation. The admin user search exposes PII (email, username) and must never be accessible to non-admin roles.

**Acceptance Criteria**:
- Accessing the admin dashboard with a regular-user session cookie returns HTTP 403.
- Admin dashboard and admin action events are logged with admin user ID and timestamp.
- Sensitive admin actions (user management, ownership reassignment) prompt for password re-entry before execution.
- Admin user search results are not accessible via the regular-user API endpoints.

---

### Feature REQ-014: User Profile Management

**ASVS Mapping**: V2.1.6, V2.5.6, V4.1.1, V5.1.1

**Updated Requirements**:
- Users can update their username (3ΓÇô50 characters, must remain unique), email address (must be valid format), and password.
- Password change requires confirmation of the current password before accepting the new password.
- New password must meet the same strength requirements as registration (ΓëÑ 12 characters, not breached).
- Email address changes trigger a verification email to the new address; the change is not applied until verified.
- Username uniqueness is validated server-side at save time.
- Profile update events are logged: user ID, fields changed (not values), timestamp.

**Securability Notes**: Profile management without current-password confirmation allows an attacker who has hijacked a session to lock the legitimate user out by changing their password or email. Email verification for address changes ensures the new address is owned by the user and preserves the password-reset path. Username uniqueness must be checked with a server-side constraint (not only a client-side check) to prevent race-condition duplicates.

**Acceptance Criteria**:
- Submitting a password change without providing the correct current password returns HTTP 400 or HTTP 403.
- An email change does not take effect until the verification link in the email to the new address is clicked.
- Changing username to an already-taken value returns a validation error.
- Profile update events are logged with changed field names (not values).

---

### Feature REQ-015: Top Rated Notes

**ASVS Mapping**: V4.1.1, V5.3.1

**Updated Requirements**:
- All users (authenticated and unauthenticated) can view the Top Rated page showing public notes sorted by average rating descending.
- Only notes with at least 3 ratings are eligible for the Top Rated list.
- Average rating and ranking are computed server-side; client-supplied sort or filter parameters are validated and ignored if not in the allowed set.
- Note previews (first 200 characters) are HTML-encoded on output.
- The Top Rated listing must only include public notes; private notes must never appear regardless of rating score.

**Securability Notes**: The Top Rated page is a public-facing endpoint. Average and ranking must be server-derived aggregates. Output encoding on previews is essential here because this is a high-visibility page: a stored XSS in a note that reaches Top Rated would affect all visitors. Parameterized sort/filter handling prevents parameter injection via query strings.

**Acceptance Criteria**:
- Private notes never appear in the Top Rated list even if they have the highest average rating.
- Notes with fewer than 3 ratings do not appear in the Top Rated list.
- HTML tags in note previews on the Top Rated page are encoded and do not execute as script.

---

### Feature REQ-016: Note Ownership Reassignment

**ASVS Mapping**: V4.3.1, V4.3.2, V7.2.1

**Updated Requirements**:
- Admin users can change the owner of any note to a different existing user.
- The target user ID must be validated server-side to be an existing active user; requests referencing nonexistent users return HTTP 400.
- Reassignment requires admin role, verified server-side.
- Admin must re-authenticate or confirm elevated session before performing reassignment.
- Reassignment is logged in full: admin user ID, note ID, previous owner user ID, new owner user ID, timestamp.
- New owner gains full ownership permissions immediately after reassignment.

**Securability Notes**: Ownership reassignment is a high-impact admin operation: it transfers control of content between users and directly affects access control decisions for all note-related features. Full audit logging of this operation is mandatory for accountability. Re-authentication requirement limits the impact of admin session compromise. Validating the target user server-side prevents orphaning notes to nonexistent user IDs, which could create integrity or access-control anomalies.

**Acceptance Criteria**:
- Performing reassignment without admin role returns HTTP 403.
- Attempting to reassign to a nonexistent user ID returns HTTP 400.
- Reassignment event is written to the application log with all four required fields (admin ID, note ID, old owner, new owner).
- After reassignment, the new owner can edit and delete the note; the old owner cannot.

---

## Cross-Cutting Securability Requirements

### CC-001: Transport Security
- All application traffic must be served over HTTPS (TLS 1.2 minimum, TLS 1.3 preferred).
- HTTP Strict Transport Security (HSTS) header must be applied with a minimum max-age of 1 year.
- Unencrypted HTTP requests are redirected to HTTPS.

### CC-002: Anti-Forgery Tokens
- All state-changing forms (POST, PUT, DELETE) include and validate ASP.NET Core anti-forgery tokens.
- API endpoints performing state changes validate the anti-forgery token or equivalent CSRF mitigation (SameSite cookie + Origin/Referer check).

### CC-003: Output Encoding
- All user-supplied content rendered in Razor views is HTML-encoded using Razor's default encoding.
- Content rendered in JavaScript contexts uses JavaScript-specific encoding (not HTML encoding).
- JSON API responses use Content-Type: application/json to prevent MIME sniffing.

### CC-004: Security Response Headers
- Content-Security-Policy header configured to restrict inline scripts and untrusted sources.
- X-Content-Type-Options: nosniff on all responses.
- X-Frame-Options: DENY or SAMEORIGIN.
- Referrer-Policy: strict-origin-when-cross-origin.

### CC-005: Error Handling
- Unhandled exceptions return a generic error page to the user; stack traces and system details are never exposed in HTTP responses.
- All unhandled exceptions are logged server-side with correlation ID.
- Custom error pages are configured for HTTP 400, 403, 404, and 500 status codes.

### CC-006: Logging and Observability
- All security-relevant events (authentication, authorization failures, admin actions, data mutations) are logged in structured format including: event type, user ID, resource ID, source IP, timestamp, result.
- Logs must never contain passwords, plaintext tokens, session identifiers, or full PII fields.
- Log entries must be immutable once written; log storage must not be accessible to application users.
- Audit log retention period: minimum 90 days.

### CC-007: Dependency and Framework Hygiene
- ASP.NET Core and Entity Framework Core packages must be kept current with security patches.
- Third-party JavaScript libraries (CDN or bundled) must be pinned to specific versions with Subresource Integrity (SRI) hashes.

### CC-008: Rate Limiting
- Authentication endpoints (login, registration, password reset) are rate-limited per IP.
- Search and share-link access endpoints are rate-limited per user and per IP.
- Rate-limit responses return HTTP 429 with a Retry-After header.

---

## Open Gaps and Assumptions

| ID | Gap or Assumption | Recommended Action |
|----|-------------------|-------------------|
| G-001 | No multi-factor authentication (MFA) is specified. For Level 2 completeness (V2.3.1), MFA should be offered even if not mandatory. | Product decision required: add optional TOTP/MFA or document as accepted risk. |
| G-002 | Share links have no expiration by default. V4.3 intent is best satisfied with a default or configurable TTL. | Add optional share link TTL; surface expiration setting in the UI. |
| G-003 | File serving mechanism (how attachments are delivered to authorized users) is not specified. Serving files through a controller endpoint with authorization checks is required. | Define file serving architecture before implementation. |
| G-004 | Breached password list implementation source (online API vs. offline list) is unspecified. | Choose implementation: k-anonymity API (HIBP) for online, or bundled list for air-gapped deployments. |
| G-005 | Admin re-authentication mechanism (full password prompt vs. elevated session flag) is unspecified. | Select implementation pattern; ASP.NET Core supports `RequireAuthenticatedUser` policy with re-authentication middleware. |
| G-006 | The PRD does not specify a Content Security Policy nonce/hash strategy for inline scripts generated by Razor. | Architecture team to define CSP policy before frontend implementation begins. |
| G-007 | Email verification infrastructure (SMTP provider, deliverability) is not addressed. | Identify email sending provider and ensure reset/verification links are tested in staging. |
| G-008 | The activity log referenced in REQ-013 (admin dashboard) is not defined as a separate entity. It is assumed to be the structured application log; if a dedicated audit-trail entity is needed, schema design is required. | Clarify whether the activity log is the application log or a separate queryable audit table. |
| G-009 | No session idle timeout value is specified beyond "timeout." | Define idle session timeout (recommended: 30 minutes for regular users, 15 minutes for admin sessions) and absolute session lifetime. |
| G-010 | The PRD assumes single-tenancy (one deployment per organization). If multi-tenancy is planned, cross-tenant isolation requirements would escalate to Level 3 in several areas. | Confirm deployment model before finalizing access control design. |

cmd.exe : 
At G:\securable-fiasse_benchmark\fiasse_benchmark\scripts\PowerShell\run-codegen-copilot-claude-plugin.ps1:329 
char:30
+             $enhanceOutput = & cmd /c "copilot " @copilotArgs 2>&1 |
+                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
Total usage est:        1 Premium request
API time spent:         4m 39s
Total session time:     4m 44s
Total code changes:     +0 -0
Breakdown by AI model:
 claude-sonnet-4.6        284.4k in, 13.4k out, 210.8k cached (Est. 1 Premium request)
