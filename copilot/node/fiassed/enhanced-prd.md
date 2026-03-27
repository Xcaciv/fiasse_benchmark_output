ΓùÅ Read copilot_prd_enhance_5qx02w1v.ihd.txt
  Γöé ~\AppData\Local\Temp\copilot_prd_enhance_5qx02w1v.ihd.txt
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

ΓùÅ List directory prd_broad
  Γöö 1 file found

ΓùÅ Search (glob)
  Γöé "**/data/**"
  Γöö No matches found

ΓùÅ List directory prd_clear
  Γöö 1 file found

ΓùÅ Read README.md
  Γöö 5 lines read

ΓùÅ Read loosenotes.prd
  Γöé prd_broad\loosenotes.prd
  Γöö 105 lines read

# Enhanced PRD: Loose Notes Web Application

## ASVS Level Decision

**Selected Level: ASVS Level 2**

This is a production multi-user web application with authenticated users, personal data (notes, profile information), role-based access control, file upload capability, and public/private content sharing. Level 1 is insufficient because the application implements trust-boundary-crossing access control decisions (public vs. private notes, share links, admin privileges) that must be systematically verified ΓÇö not just surface-checked. Level 3 is not required because the system does not process regulated sensitive data (health, financial) and does not operate under elevated regulatory mandates.

Feature-level escalations beyond baseline:
- **REQ-005** (File Attachment): file content validation and storage isolation require Level 2+ controls.
- **REQ-013** (Admin Dashboard): admin re-authentication and action logging require Level 2+ controls.

---

## Feature-ASVS Coverage Matrix

| Feature | ASVS Section | Requirement ID | Level | Coverage | PRD Change Needed |
|---------|-------------|----------------|-------|----------|-------------------|
| REQ-001 | V2.1 | 2.1.1 | 1 | Missing | Specify min 12 chars, max 128 chars; reject common passwords |
| REQ-001 | V2.1 | 2.1.2 | 1 | Missing | Add explicit minimum password length of 12 characters |
| REQ-001 | V5.1 | 5.1.3 | 1 | Partial | Add server-side username/email format and uniqueness validation |
| REQ-001 | V2.2 | 2.2.2 | 1 | Missing | Non-revealing error messages to prevent account enumeration |
| REQ-002 | V3.2 | 3.2.1 | 1 | Missing | Issue new session token on successful authentication |
| REQ-002 | V3.4 | 3.4.1 | 1 | Missing | Set Secure, HttpOnly, SameSite=Strict on session cookies |
| REQ-002 | V3.3 | 3.3.1 | 1 | Partial | Logout must invalidate server-side session, not just clear cookie |
| REQ-002 | V2.2 | 2.2.1 | 1 | Missing | Rate-limit login endpoint; define lockout or progressive delay |
| REQ-003 | V2.5 | 2.5.6 | 1 | Covered | Time-limited single-use token present |
| REQ-003 | V2.5 | 2.5.3 | 2 | Missing | Store reset token as secure hash server-side, not plaintext |
| REQ-003 | V7.1 | 7.1.1 | 1 | Missing | Log password reset requests and completions |
| REQ-003 | V2.2 | 2.2.2 | 1 | Missing | Identical response regardless of whether email is registered |
| REQ-004 | V5.1 | 5.1.3 | 1 | Missing | Define max length for title (Γëñ255) and content (Γëñ65535) |
| REQ-004 | V4.2 | 4.2.1 | 1 | Partial | Explicitly derive owner ID from session, not form input |
| REQ-004 | V5.3 | 5.3.3 | 1 | Partial | Confirm Razor HTML encoding in all render paths |
| REQ-005 | V12.1 | 12.1.1 | 1 | Partial | Define and enforce explicit file size limit server-side |
| REQ-005 | V12.1 | 12.1.2 | 2 | Missing | Validate file content/magic bytes, not extension only |
| REQ-005 | V12.3 | 12.3.1 | 1 | Missing | Store files outside web root; never serve direct file paths |
| REQ-005 | V12.3 | 12.3.3 | 1 | Missing | Reject archive formats (ZIP, TAR, GZ) even if renamed |
| REQ-005 | V12.5 | 12.5.1 | 1 | Missing | Serve downloads with Content-Disposition: attachment + nosniff |
| REQ-006 | V4.2 | 4.2.1 | 1 | Covered | Ownership check present |
| REQ-006 | V5.1 | 5.1.3 | 1 | Partial | Validate updated title/content server-side on every save |
| REQ-006 | V13.2 | 13.2.3 | 1 | Partial | Verify anti-forgery token on every note edit POST |
| REQ-007 | V4.2 | 4.2.1 | 1 | Covered | Owner/admin check present |
| REQ-007 | V13.2 | 13.2.3 | 1 | Partial | CSRF token required on deletion POST; GET must not delete |
| REQ-008 | V4.1 | 4.1.3 | 1 | Missing | Share tokens must be cryptographically random ΓëÑ128-bit entropy |
| REQ-008 | V3.5 | 3.5.2 | 2 | Covered | Token revocation mentioned |
| REQ-008 | V8.3 | 8.3.4 | 2 | Missing | Store share token hash only; raw token never recoverable from DB |
| REQ-009 | V4.1 | 4.1.2 | 1 | Covered | Visibility enforced server-side |
| REQ-009 | V4.2 | 4.2.1 | 1 | Partial | Clarify 404 vs 403 response for private note access (existence leakage) |
| REQ-010 | V4.2 | 4.2.1 | 1 | Missing | Enforce one-rating-per-user and no self-rating server-side |
| REQ-010 | V5.1 | 5.1.3 | 1 | Missing | Validate rating integer 1ΓÇô5 server-side; reject out-of-range values |
| REQ-011 | V4.2 | 4.2.1 | 1 | Partial | Confirm rating detail list restricted to note owner server-side |
| REQ-012 | V5.1 | 5.1.1 | 1 | Missing | Length-limit and validate search query; enforce parameterized queries |
| REQ-012 | V4.1 | 4.1.2 | 1 | Covered | Visibility filtering present |
| REQ-013 | V4.3 | 4.3.1 | 1 | Partial | Enforce admin role server-side on every request, not just routing |
| REQ-013 | V4.3 | 4.3.2 | 2 | Missing | Require re-authentication before destructive admin actions |
| REQ-013 | V7.1 | 7.1.2 | 1 | Covered | Admin action logging present |
| REQ-014 | V4.2 | 4.2.1 | 1 | Missing | Require server-side check that user edits only own profile |
| REQ-014 | V2.1 | 2.1.2 | 1 | Missing | Password change must enforce same policy as registration |
| REQ-014 | V2.1 | 2.1.5 | 2 | Missing | Require current password confirmation before allowing change |
| REQ-015 | V4.1 | 4.1.2 | 1 | Covered | Public-only filter in query |
| REQ-015 | V8.1 | 8.1.3 | 2 | Missing | Set cache-control headers; avoid caching sensitive aggregated data |
| REQ-016 | V4.3 | 4.3.2 | 2 | Partial | Log before/after owner IDs; add CSRF protection to POST action |

---

## Enhanced Feature Specifications

### Feature REQ-001: User Registration

**ASVS Mapping**: V2.1.1, V2.1.2, V2.2.2, V5.1.3

**Updated Requirements**:
- Users register with username, email address, and password.
- **Password policy**: minimum 12 characters, maximum 128 characters; common passwords (top-1000 or NIST bad-password list equivalent) must be rejected at registration with a user-visible message.
- Username: validated server-side for format (alphanumeric and limited special characters; max 64 chars); must be unique.
- Email: validated server-side for format; must be unique in the system.
- Duplicate username or email registration must return a **generic message** that does not confirm whether the specific identifier is already registered (prevents account enumeration).
- Registration creates a user account with default role = User.
- Registration attempts (success and failure) are logged with: timestamp, IP address, attempted username (not password).

**Securability Notes**: All validation must occur server-side regardless of client-side checks ΓÇö the registration form is a trust boundary where attacker-controlled input enters the system. Enforcing a minimum 12-character password and common-password rejection at this boundary dramatically reduces credential-stuffing susceptibility (Trustworthiness/Resilience). Generic error messages on duplicate registration protect existing users' account existence from enumeration (Confidentiality). Structured logging of registration events supports accountability and abuse-pattern detection without persisting credential material.

---

### Feature REQ-002: User Login / Authentication

**ASVS Mapping**: V2.2.1, V3.2.1, V3.3.1, V3.4.1, V7.1.1

**Updated Requirements**:
- Users authenticate with username (or email) and password.
- On successful authentication, a **new session token is issued**; any prior session token for that user is invalidated (prevents session fixation).
- Session cookie must be set with `Secure`, `HttpOnly`, and `SameSite=Strict` attributes.
- **Login rate limiting**: ΓëÑ5 consecutive failed attempts for the same account within 15 minutes triggers a progressive delay (minimum 30 seconds) or temporary lockout; lockout state is shown to the user with an expected unlock time.
- Logout must **invalidate the server-side session record**; a re-presented invalidated token must receive an HTTP 401 response. Clearing only the client cookie is insufficient.
- Session inactivity timeout: configurable per deployment (default Γëñ30 minutes idle).
- Authentication events (success, failure, lockout, logout) are logged with: timestamp (UTC), username/attempted-username, IP address, event type.

**Securability Notes**: Issuing a fresh session token at login and destroying the prior one prevents session-fixation attacks, which are trivially exploitable when session tokens persist across authentication. Cookie hardening (Secure, HttpOnly, SameSite) eliminates the majority of passive session-hijacking surface. Server-side session invalidation at logout is the only reliable session termination guarantee ΓÇö client-side cookie deletion can be bypassed. Rate limiting login attempts is the primary control against credential-stuffing, which is the most prevalent attack against multi-user web applications. Structured auth event logging is essential for accountability and incident response.

---

### Feature REQ-003: Password Reset

**ASVS Mapping**: V2.2.2, V2.5.3, V2.5.6, V2.7.1, V7.1.1

**Updated Requirements**:
- Password reset is initiated by entering an email address. The **response message must be identical** whether or not the email is registered (no account enumeration).
- System generates a cryptographically random reset token (ΓëÑ128 bits entropy, URL-safe encoding) and stores only its **secure hash** in the database; the raw token is delivered once via email.
- Token is valid for 1 hour from issuance; token is single-use (invalidated immediately on successful password change or expiry).
- New password must meet the same policy as registration (min 12 chars, common-password rejection).
- Expired, used, or invalid tokens return a generic error without revealing the specific reason.
- Password reset requests and completions are logged with: timestamp, IP address, obscured identifier (e.g., hashed email); passwords and raw tokens are never logged.

**Securability Notes**: Storing only the token hash server-side means a database compromise does not yield valid reset tokens ΓÇö analogous to password hashing (Confidentiality, Integrity). Cryptographically random token generation makes guessing attacks computationally infeasible. Single-use and time-limited tokens bound the exploitation window if a reset email is intercepted or forwarded. Identical response messages protect existing account existence. Log entries for reset events without raw token values achieve accountability without creating an exploitable log artifact.

---

### Feature REQ-004: Note Creation

**ASVS Mapping**: V4.2.1, V5.1.3, V5.3.3, V7.1.1

**Updated Requirements**:
- Authenticated users may create notes with title (required, max 255 chars) and content (required, max 65,535 chars or configured limit).
- Title and content must be **validated and length-checked server-side**; requests exceeding limits return HTTP 400.
- Note owner ID is **derived from the authenticated session** (not from any form field or URL parameter).
- Default visibility is `private`; creation timestamp is set server-side (not from client input).
- Note title and content must be **HTML-encoded in all Razor render paths** (list, detail, search result, share view); no raw user HTML injected into the DOM without explicit, scoped sanitization.
- Note creation events are logged with: user ID, note ID, timestamp.

**Securability Notes**: Deriving the owner ID from the authenticated session rather than accepting it from form input is a derived-integrity principle ΓÇö the server is the authoritative source for ownership attribution, preventing an attacker from creating notes on behalf of other users by manipulating hidden fields. Server-side length validation prevents storage exhaustion and excessive-payload DoS. Consistent HTML encoding across all render contexts (not just the primary detail view) ensures XSS cannot be introduced when note content appears in aggregated views such as search results or the top-rated list.

---

### Feature REQ-005: File Attachment

**ASVS Mapping**: V12.1.1, V12.1.2, V12.3.1, V12.3.3, V12.5.1

**Updated Requirements**:
- Supported upload formats: PDF, DOC, DOCX, TXT, PNG, JPG, JPEG.
- File validation must perform **both extension check and MIME type/magic byte verification** server-side; mismatches are rejected with HTTP 400.
- Archive formats (ZIP, TAR, GZ, RAR, 7Z) are never accepted, even with an allowed extension.
- Maximum file size: defined per deployment (default 10 MB); enforced server-side before content processing.
- Uploaded files are stored **outside the web root** in a directory not directly served by the web server.
- Stored files use unique, non-guessable identifiers (UUID or cryptographically random name); original filename is preserved in database metadata only.
- Files are served **exclusively through a controller action** that enforces authorization, never by directly exposing storage paths.
- Download responses must include:
  - `Content-Disposition: attachment; filename="<sanitized-original-filename>"`
  - `Content-Type` derived from validated file type (not from stored extension)
  - `X-Content-Type-Options: nosniff`
- File download authorization: user must be the note owner, holder of a valid share token, or the note must be public.
- File upload and download events are logged with: user ID (or "anonymous" for share-link access), note ID, file ID, timestamp.

**Securability Notes**: Magic-byte validation alongside extension checking prevents polyglot files and malicious content disguised with legitimate extensions ΓÇö extension-only checks are trivially bypassed (Integrity, Resilience). Storing files outside the web root eliminates direct path-based access and execution risk, which is the principal danger of any file upload feature. Forcing downloads through a controller action is what enables consistent authorization enforcement, correct Content-Type delivery, and audit logging ΓÇö it also prevents direct URL enumeration of uploaded files. These controls collectively address the highest-risk feature surface in the application.

---

### Feature REQ-006: Note Editing

**ASVS Mapping**: V4.2.1, V5.1.3, V5.3.3, V13.2.3

**Updated Requirements**:
- Note owners may modify title, content, and visibility status.
- **Server-side authorization check on every edit request**: authenticated user ID must match note owner ID; HTTP 403 returned on mismatch (prevents IDOR via modified note IDs).
- CSRF protection (anti-forgery token) must be validated on every state-changing POST/PUT.
- Title and content are validated against the same size constraints as REQ-004 on each save.
- Last-modified timestamp is set server-side (not derived from client input).
- Edit events are logged with: user ID, note ID, fields changed (not content values), timestamp.

**Securability Notes**: The server-side authorization check on every request (not only initial page load) is the critical control ΓÇö IDOR vulnerabilities arise precisely when the check is performed only once or not at all on mutating requests. CSRF token validation ensures that form submissions originate from the application's own authenticated frontend, not from cross-origin script. Deriving the modified timestamp server-side prevents timestamp manipulation attacks that could be used to forge note history.

---

### Feature REQ-007: Note Deletion

**ASVS Mapping**: V4.2.1, V4.2.2, V13.2.3

**Updated Requirements**:
- Only the note owner or an admin may delete a note; server-side authorization check required on every deletion request.
- Deletion must be triggered by a **POST request** (not GET); a UI confirmation prompt precedes the POST submission.
- Anti-forgery token must be validated on the deletion POST action.
- Deletion cascades to: all attachment files (physically removed from storage), attachment database records, associated ratings, and share links.
- Deletion events are logged with: actor user ID, note ID, timestamp.
- Admin deletion of another user's note is additionally logged with: admin user ID, note owner user ID, note ID, timestamp.

**Securability Notes**: Requiring a POST with an anti-forgery token for deletion prevents CSRF-triggered deletion via a malicious link or image tag. Server-side ownership/admin check on every deletion request prevents IDOR exploitation. Cascading physical deletion of attachment files (not just database records) prevents orphaned files from accumulating in storage and potentially being accessed through guessable paths. Separate logging of admin deletions with full actor/target context supports accountability for privileged destructive actions.

---

### Feature REQ-008: Note Sharing

**ASVS Mapping**: V3.5.2, V4.1.3, V8.3.4

**Updated Requirements**:
- Share link tokens must be **cryptographically random with ΓëÑ128 bits of entropy** (e.g., 32-byte URL-safe base64 via `RandomNumberGenerator`).
- Only the **hash of the share token** is stored server-side; the raw token is delivered once and is not recoverable from the application.
- Share link access grants read-only permission to note content and attachments; no write, delete, or rating operations are permitted via share link.
- Owner may revoke a share link at any time; revocation immediately invalidates the stored hash.
- Owner may regenerate a share link; the previous token hash is deleted before the new one is created.
- Share link access events are logged with: token reference (first 8 chars of hash), note ID, accessor IP, timestamp; full token is never logged.
- If the note is deleted, share links return HTTP 404.

**Securability Notes**: Cryptographically random, high-entropy tokens make share link enumeration attacks computationally infeasible regardless of the total number of links in the system. Storing only the hash (analogous to password hashing) means a database compromise does not yield exploitable share tokens (Confidentiality). Limiting share link permissions to read-only implements least-privilege scoping ΓÇö sharing a note should not implicitly grant capabilities the owner did not intend. Logging by token-hash reference (not full value) enables incident tracing without creating a log artifact that could be used to replay access.

---

### Feature REQ-009: Public / Private Notes

**ASVS Mapping**: V4.1.1, V4.1.2, V4.2.1

**Updated Requirements**:
- Notes have two visibility states: `public` and `private`; default at creation is `private`.
- Visibility is enforced **server-side on every request** across all access paths: direct URL, search, share link, top-rated list, and API.
- Visibility state is stored server-side and cannot be overridden by URL parameters, headers, or form fields.
- Private notes accessed by non-owners return **HTTP 404** (not 403) to prevent existence leakage.
- Search results must enforce visibility rules in the database query layer, not as a post-filter.
- Visibility change events are logged with: note ID, previous value, new value, user ID, timestamp.

**Securability Notes**: Enforcing visibility in the database query (not as application-layer post-filtering) ensures that even defects in application logic cannot expose private notes through search or aggregated views. Returning HTTP 404 for unauthorized access to private notes prevents an attacker from enumerating note IDs to map the presence of private content (Confidentiality). The visibility toggle is a trust-boundary decision that must be re-evaluated on every request, not cached or assumed from session state.

---

### Feature REQ-010: Note Rating

**ASVS Mapping**: V4.2.1, V5.1.3, V13.2.3

**Updated Requirements**:
- Only authenticated users may submit ratings; unauthenticated requests are rejected with HTTP 401.
- Rating value must be **validated server-side** as an integer between 1 and 5 inclusive; other values return HTTP 400.
- **One rating per user per note** is enforced via a database unique constraint on (user_id, note_id) in the Ratings table; server-side application logic must also check for an existing rating before insert.
- Note owners **cannot rate their own notes**; server-side check compares authenticated user ID against note owner ID.
- Optional comment is validated for max length (e.g., 1,000 chars) server-side.
- Anti-forgery token required on rating submission and edit requests.
- Rating events are logged with: user ID, note ID, rating value, action (create/update), timestamp.

**Securability Notes**: Enforcing one-rating-per-user and no self-rating via both database constraints and application logic ensures these rules survive code refactoring ΓÇö application logic alone can be bypassed if the constraint does not exist at the data layer (Integrity). Server-side integer range validation (1ΓÇô5) prevents average-rating manipulation through out-of-range values. CSRF protection on rating forms prevents cross-origin rating injection that could be used to manipulate public note rankings.

---

### Feature REQ-011: Rating Management

**ASVS Mapping**: V4.1.2, V4.2.1

**Updated Requirements**:
- Only the note owner may access the full rating detail list; server-side authorization check required (HTTP 403 for others).
- Rating detail list includes: rating value, comment, rater username, and timestamp. Rater email address and user ID must **not** be exposed.
- Average rating is **calculated server-side** in the query; not derived from any client-supplied value.
- Ratings are sorted by date (newest first) server-side; sort order is not overridable by client parameters without validation.

**Securability Notes**: Restricting the full rater identity list to the note owner protects rater privacy ΓÇö public-facing views should show aggregate data only (Confidentiality). Computing the average server-side is a derived-integrity control; accepting a client-submitted average would allow trivial rating manipulation. Ensuring rater email is excluded from the management view limits unnecessary PII exposure in a higher-privilege view.

---

### Feature REQ-012: Note Search

**ASVS Mapping**: V4.1.2, V5.1.1, V5.3.4

**Updated Requirements**:
- Search is available to all users (authenticated and unauthenticated) subject to visibility rules.
- Search query must be **length-limited server-side** (max 200 chars); requests exceeding the limit return HTTP 400.
- Search is implemented using **parameterized EF Core queries**; no dynamic SQL string construction from user input is permitted.
- Visibility enforcement in the query:
  - Authenticated: own notes (any visibility) + public notes from others.
  - Unauthenticated: public notes only.
  - Private notes from other users: never returned, enforced in the query predicate.
- Search result excerpts are HTML-encoded in all output views; no raw user content injected.
- Search endpoint should be rate-limited to reduce bulk content-harvesting risk.

**Securability Notes**: Parameterized queries via EF Core are the definitive defense against SQL injection in the search path. Visibility enforcement at the database query layer (not post-filtering) ensures search results cannot leak private notes due to application-layer bypasses. HTML encoding of excerpts prevents XSS when user-generated content is rendered in search result listings. Input length limits constrain query complexity and reduce potential DoS via expensive full-text search patterns.

---

### Feature REQ-013: Admin Dashboard

**ASVS Mapping**: V4.3.1, V4.3.2, V7.1.1, V7.1.2

**Updated Requirements**:
- Admin dashboard is accessible only to users with the Admin role; server-side role check required on every controller action (not route-level only).
- All admin actions must verify the Admin role from the authenticated session; no reliance on client-supplied headers or session attributes the user can modify.
- **Re-authentication** (current password confirmation) is required before performing destructive or identity-modifying admin actions (user role changes, note deletion, ownership reassignment).
- Dashboard data displayed: total user count, total note count, recent activity log (last 50 events, paginated). Password hashes, tokens, and raw session data must never be returned.
- User list exposes: username, email, registration date, note count, role assignment.
- User search by username or email is validated and parameterized server-side.
- All admin actions are logged with: admin user ID, action type, target resource ID, timestamp; logs are append-only.

**Securability Notes**: Admin endpoints represent the highest-privilege trust boundary in the application and require defense-in-depth. Re-authentication before destructive actions reduces the blast radius of a compromised admin session ΓÇö session hijacking or CSRF attacks cannot trigger irreversible actions without an additional credential step (Trustworthiness, Resilience). Append-only logging of every admin action with full actor and target context is essential for accountability; without it, privilege abuse and data access cannot be reconstructed during incident investigation. The admin interface must never surface credential material even incidentally.

---

### Feature REQ-014: User Profile Management

**ASVS Mapping**: V2.1.2, V2.1.5, V4.2.1, V7.1.1

**Updated Requirements**:
- Users may update their own username, email address, and password.
- **Server-side check required**: the authenticated user ID from the session determines which profile may be edited; no user ID accepted from form fields or URL parameters.
- **Current password must be confirmed** before accepting a new password; new password must meet the same policy as registration (min 12 chars, common-password rejection).
- Email address change requires format and uniqueness validation; a notification email is sent to the **previous email address** upon successful change.
- Username change requires format and uniqueness validation.
- Profile changes are logged with: user ID, field changed (category only, e.g., "email", "password"), timestamp; old/new values for credential fields must not be logged.

**Securability Notes**: Requiring current password confirmation before a password change prevents an attacker who has hijacked an active session from silently taking over the account (Trustworthiness). Server-side derivation of the edit target from the session user ID prevents IDOR on profile endpoints ΓÇö an attacker cannot modify another user's profile by changing a hidden user ID field. Notifying the previous email on address change provides the legitimate user an out-of-band signal that their account is being modified, enabling them to detect and respond to unauthorized changes (Accountability).

---

### Feature REQ-015: Top Rated Notes

**ASVS Mapping**: V4.1.2, V5.3.3, V8.1.3

**Updated Requirements**:
- Top Rated page displays only **public** notes; visibility filter applied in the database query, not as a post-filter.
- Minimum 3 ratings threshold enforced in the query predicate.
- Average rating is calculated server-side in the query.
- All displayed user-generated content (title, author, excerpt) is HTML-encoded in output views.
- Response must include `Cache-Control: no-store` or a short `max-age` with `must-revalidate` to prevent stale data being served from proxy caches.

**Securability Notes**: Filtering public-only notes at the query layer (derived integrity) ensures that no private notes can appear in the public list due to application logic errors. Cache-control headers prevent intermediate proxies from serving an old version of the list to a user who might see notes from a period before a visibility change was applied. HTML encoding of all aggregated user content prevents stored XSS from appearing in a high-visibility page.

---

### Feature REQ-016: Note Ownership Reassignment

**ASVS Mapping**: V4.2.1, V4.3.2, V7.1.2

**Updated Requirements**:
- Only Admin role users may invoke note ownership reassignment; server-side role check required.
- Admin must provide: note ID and target user ID; both validated server-side.
- Target user must exist and be an active account; reassignment to non-existent or deactivated users is rejected.
- Anti-forgery token required on the reassignment POST action.
- **Re-authentication** (current admin password) required before completing reassignment.
- Reassignment event is logged with: admin user ID, note ID, previous owner user ID, new owner user ID, timestamp; this log entry is treated as immutable (append-only log store).
- Admin self-assignment requires an explicit additional confirmation step.

**Securability Notes**: Full before/after ownership audit logging is essential for this feature because ownership reassignment directly affects data attribution and access control ΓÇö without an immutable record, reassignments cannot be reviewed or reversed (Accountability). Re-authentication before completing the action ensures that CSRF attacks or session hijacking cannot silently execute ownership changes at scale. Append-only log storage prevents a compromised admin account from covering its tracks by deleting log entries after performing unauthorized reassignments.

---

## Cross-Cutting Securability Requirements

### CC-001: Transport Security
- All HTTP traffic must be redirected to HTTPS at the application or reverse-proxy layer.
- `Strict-Transport-Security: max-age=31536000; includeSubDomains` header must be set on all HTTPS responses.
- TLS 1.2 minimum; TLS 1.3 preferred. TLS 1.0 and 1.1 and weak cipher suites (RC4, 3DES, export-grade) must be disabled.
- **ASVS**: V9.1.1, V9.1.2

### CC-002: Security Response Headers
All responses must include:
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Content-Security-Policy` restricting inline scripts, `eval`, and external resource origins to an allowlist
- `Referrer-Policy: strict-origin-when-cross-origin`
- **ASVS**: V14.4.1, V14.4.2, V14.4.3

### CC-003: CSRF Protection
- Anti-forgery tokens must be validated on all state-changing HTTP requests (POST, PUT, PATCH, DELETE).
- ASP.NET Core's `[ValidateAntiForgeryToken]` attribute must be applied globally or at every applicable controller action; opt-out exceptions must be documented and justified.
- **ASVS**: V4.2.3, V13.2.3

### CC-004: Error Handling
- The application must not expose stack traces, internal file paths, connection strings, or framework version details in HTTP responses to end users.
- Generic error pages must be served for 4xx and 5xx responses.
- Detailed error information is logged server-side only.
- **ASVS**: V7.4.1, V14.3.2

### CC-005: Logging and Audit Trail
- All security-relevant events must be logged with: timestamp (UTC), user ID or "anonymous", IP address, event type, resource ID.
- Logs must not contain plaintext passwords, session tokens, password reset tokens, or share link raw values.
- Log entries must be write-protected (append-only destination or immutable storage).
- Minimum log retention: 90 days (or per applicable policy).
- **ASVS**: V7.1.1, V7.1.2, V7.2.1

### CC-006: Password Hashing Configuration
- ASP.NET Core Identity PBKDF2 work factor must be explicitly configured to a minimum of 310,000 iterations for HMAC-SHA256, aligned with current NIST SP 800-132 guidance.
- Work factor configuration must be documented and periodically reviewed.
- **ASVS**: V2.4.1

### CC-007: Dependency and Platform Security
- ASP.NET Core and Entity Framework Core versions must be kept current with security patches.
- Automated dependency vulnerability scanning (`dotnet list package --vulnerable`) should be integrated into CI/CD.
- **ASVS**: V14.2.1

### CC-008: Database Integrity
- All user-supplied values passed to EF Core queries must use parameterized expressions; raw SQL string interpolation from user input is prohibited.
- Foreign key constraints are enforced at the database layer, not application logic alone.
- **ASVS**: V5.3.4, V5.3.5

### CC-009: Input Size and Rate Limiting
- Global request body size limit enforced at middleware level.
- Sensitive endpoints (login, registration, password reset request, search) must be rate-limited per IP and/or per account using ASP.NET Core rate limiting middleware or a reverse proxy.
- **ASVS**: V5.1.4, V11.1.1

---

## Open Gaps and Assumptions

| ID | Gap / Assumption | Recommended Action |
|----|------------------|--------------------|
| GAP-001 | No multi-factor authentication (MFA) is specified. ASVS L2 recommends MFA for accounts accessing sensitive features. | Decide whether to include optional TOTP-based MFA; document absence as an accepted risk if deferred. |
| GAP-002 | Email delivery mechanism for password reset is unspecified. Third-party email providers introduce a data-handling trust boundary. | Document chosen email service and confirm a data processing agreement is in place. |
| GAP-003 | File storage backend (local vs. cloud) is not finalized. Cloud storage introduces signed-URL expiry and access-policy requirements. | Specify storage provider; add signed-URL expiry and access-control requirements for cloud scenarios. |
| GAP-004 | No explicit account lockout policy is defined beyond rate limiting. | Define lockout threshold, duration, and admin unlock process. |
| GAP-005 | Admin role assignment mechanism is unspecified. Uncontrolled in-app role promotion is a critical risk. | Define how the Admin role is granted (e.g., seeded via configuration or CLI only; not via the application UI). |
| GAP-006 | No data retention or account deletion policy is defined. GDPR or other privacy regulations may require right-to-erasure. | Define data retention policy and a user account deletion/anonymization flow. |
| GAP-007 | Share links grant unauthenticated access to potentially sensitive note content. This is an explicit design decision. | Document as an accepted business decision; consider adding optional share-link expiry time as a future feature. |
| GAP-008 | Content Security Policy directives are not defined. Razor views with dynamic content require a tested CSP. | Define and test CSP headers; deploy in report-only mode in staging before enforcement. |
| GAP-009 | The activity log shown in REQ-013 does not define which events are included or retained. | Produce an explicit event catalog for the admin activity log. |
| GAP-010 | PBKDF2 iteration count for ASP.NET Core Identity is not specified, leaving the default (which may be below current guidance) in place. | Explicitly configure and document a minimum iteration count of ΓëÑ310,000 for HMAC-SHA256 per NIST SP 800-132. |

cmd.exe : 
At G:\securable-fiasse_benchmark\fiasse_benchmark\scripts\PowerShell\run-codegen-copilot-claude-plugin.ps1:329 
char:30
+             $enhanceOutput = & cmd /c "copilot " @copilotArgs 2>&1 |
+                              ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    + CategoryInfo          : NotSpecified: (:String) [], RemoteException
    + FullyQualifiedErrorId : NativeCommandError
 
Total usage est:        1 Premium request
API time spent:         6m 43s
Total session time:     6m 48s
Total code changes:     +0 -0
Breakdown by AI model:
 claude-sonnet-4.6        252.4k in, 21.0k out, 173.4k cached (Est. 1 Premium request)
