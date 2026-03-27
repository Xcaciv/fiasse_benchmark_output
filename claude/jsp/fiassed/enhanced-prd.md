# Project Requirements Document ΓÇö Loose Notes Web Application (Securability-Enhanced)

**Baseline Document Version**: 1.0
**Enhanced By**: FIASSE Securable Engineering Plugin (SSEM + ASVS)
**Enhancement Date**: 2026-03-26
**ASVS Baseline Level**: Level 2

---

## ASVS Level Decision

### Selected Level: **ASVS Level 2**

**Rationale**: The Loose Notes application is a production multi-user web platform with authenticated identity, persistent personal data (notes, email addresses, usernames), file upload and storage, admin privilege escalation paths, and public share-link exposure of user content. These characteristics exceed the Level 1 threshold.

**Why Level 1 is insufficient**:
- The application stores user-owned content and PII (email, username) requiring tested security controls, not best-effort
- File upload attack surface demands verified validation and storage isolation
- Admin role with note reassignment and user management constitutes a privileged access path
- Public share links expose content to unauthenticated actors without author consent revocation controls being tested
- Password reset tokens are high-value credential assets requiring full lifecycle validation

**Why Level 3 is not required**:
- No regulated PII categories (health, financial, government identity)
- No high-consequence operations (financial transactions, physical system control)
- No stated compliance framework requiring formal verification (FedRAMP, PCI DSS, HIPAA)

**Feature-level escalations**: F-05 (File Attachment) and F-08 (Note Sharing) warrant Level 2+ scrutiny given unauthenticated access paths and file execution risk. F-13 (Admin Dashboard) warrants elevated scrutiny on access control enforcement.

---

## Feature-ASVS Coverage Matrix

| Feature | ASVS Section | Requirement ID | Level | Coverage | PRD Change Needed |
|---------|-------------|----------------|-------|----------|-------------------|
| F-01 User Registration | V2.1 | 2.1.1ΓÇô2.1.12 | 2 | Partial | Add password complexity, length, and breach-list check requirements |
| F-01 User Registration | V2.4 | 2.4.1ΓÇô2.4.5 | 2 | Covered | PBKDF2 via ASP.NET Identity referenced |
| F-01 User Registration | V5.1 | 5.1.3 | 2 | Missing | Add server-side input validation for username and email fields |
| F-01 User Registration | V8.3 | 8.3.4 | 2 | Missing | Add PII minimization statement: collect only username, email, password |
| F-02 Login/Auth | V2.2 | 2.2.1ΓÇô2.2.2 | 2 | Partial | Add account lockout/throttling after N failed attempts |
| F-02 Login/Auth | V3.1 | 3.1.1 | 2 | Missing | Session token must never appear in URLs |
| F-02 Login/Auth | V3.3 | 3.3.1ΓÇô3.3.4 | 2 | Partial | Define explicit session timeout values (idle and absolute) |
| F-02 Login/Auth | V3.4 | 3.4.1ΓÇô3.4.5 | 2 | Missing | Cookie attributes: Secure, HttpOnly, SameSite=Lax/Strict |
| F-02 Login/Auth | V7.1 | 7.1.1ΓÇô7.1.2 | 2 | Partial | Log failed auth attempts with timestamp, IP; no credentials logged |
| F-03 Password Reset | V2.5 | 2.5.1ΓÇô2.5.6 | 2 | Partial | Clarify token entropy, single-use invalidation, and expiry enforcement |
| F-03 Password Reset | V7.1 | 7.1.1 | 2 | Missing | Log password reset requests and completions |
| F-04 Note Creation | V4.2 | 4.2.1 | 2 | Partial | Explicit server-side ownership binding; user ID from session, not form input |
| F-04 Note Creation | V5.1 | 5.1.3ΓÇô5.1.4 | 2 | Missing | Server-side validation for title length and content size limits |
| F-04 Note Creation | V5.3 | 5.3.1ΓÇô5.3.3 | 2 | Missing | Output encoding requirements for rendered note content |
| F-05 File Attachment | V12.1 | 12.1.1ΓÇô12.1.3 | 2 | Partial | Add max file size, per-user storage quota, and malicious file controls |
| F-05 File Attachment | V12.2 | 12.2.1 | 2 | Missing | Validate file content by magic bytes, not only extension |
| F-05 File Attachment | V12.3 | 12.3.1ΓÇô12.3.6 | 2 | Missing | Prevent path traversal; sanitize stored filename; disable execution of uploaded files |
| F-05 File Attachment | V12.4 | 12.4.1ΓÇô12.4.2 | 2 | Missing | Store files outside web root; permissions restrict direct access |
| F-05 File Attachment | V12.5 | 12.5.1ΓÇô12.5.2 | 2 | Missing | Set Content-Disposition: attachment on download; restrict served MIME types |
| F-06 Note Editing | V4.2 | 4.2.1 | 2 | Partial | Verify server-side ownership on every edit request; do not trust client-supplied noteId ownership claim |
| F-06 Note Editing | V11.1 | 11.1.1ΓÇô11.1.2 | 2 | Missing | Validate that visibility state transitions are intentional; add confirmation on visibility change to public |
| F-07 Note Deletion | V4.2 | 4.2.1 | 2 | Covered | Owner/admin check stated |
| F-07 Note Deletion | V11.1 | 11.1.4 | 2 | Missing | Prevent rapid repeated deletion requests (anti-automation) |
| F-07 Note Deletion | V7.1 | 7.1.2 | 2 | Missing | Audit log deletion events with actor, timestamp, note ID |
| F-08 Note Sharing | V4.1 | 4.1.3 | 2 | Missing | Deny-by-default: validate share token server-side on every request |
| F-08 Note Sharing | V3.5 | 3.5.2ΓÇô3.5.3 | 2 | Missing | Share tokens must use cryptographically random values; define minimum entropy |
| F-08 Note Sharing | V8.3 | 8.3.1 | 2 | Missing | Ensure unauthenticated share access does not expose metadata beyond note content |
| F-08 Note Sharing | V11.1 | 11.1.6 | 2 | Missing | Rate-limit share link generation per user |
| F-09 Public/Private Notes | V4.2 | 4.2.1 | 2 | Covered | Private visibility enforcement stated |
| F-09 Public/Private Notes | V5.1 | 5.1.1 | 2 | Missing | Server-side enforcement of visibility; client toggle must be validated server-side |
| F-10 Note Rating | V4.2 | 4.2.1 | 2 | Missing | Verify authenticated user owns the rating before allowing edit |
| F-10 Note Rating | V11.1 | 11.1.1ΓÇô11.1.3 | 2 | Missing | Rate-limit rating submissions per user per note per time window |
| F-10 Note Rating | V5.1 | 5.1.3 | 2 | Missing | Validate rating value is integer 1ΓÇô5; validate comment length and character set |
| F-11 Rating Management | V4.2 | 4.2.1 | 2 | Missing | Verify note ownership before exposing full rating list |
| F-12 Note Search | V4.2 | 4.2.1 | 2 | Covered | Private note filtering stated |
| F-12 Note Search | V5.1 | 5.1.3 | 2 | Missing | Server-side validation and length limit on search query string |
| F-12 Note Search | V5.3 | 5.3.4 | 2 | Missing | ORM parameterization for search query (already implied via EF, make explicit) |
| F-13 Admin Dashboard | V4.3 | 4.3.1ΓÇô4.3.2 | 2 | Partial | Enforce admin role check at every admin endpoint, not just the dashboard entry |
| F-13 Admin Dashboard | V7.1 | 7.1.1ΓÇô7.1.2 | 2 | Partial | Admin actions must produce structured audit logs with actor identity |
| F-13 Admin Dashboard | V4.1 | 4.1.5 | 2 | Missing | Admin access control failures must be logged and alerted |
| F-14 User Profile Mgmt | V2.1 | 2.1.1ΓÇô2.1.5 | 2 | Missing | Password change must require current password re-verification |
| F-14 User Profile Mgmt | V2.6 | 2.6.1 | 2 | Missing | Username/email change must require re-authentication |
| F-14 User Profile Mgmt | V7.1 | 7.1.2 | 2 | Missing | Log profile changes (username, email, password) with timestamp |
| F-15 Top Rated Notes | V4.2 | 4.2.1 | 2 | Covered | Public-only filter stated |
| F-15 Top Rated Notes | V11.1 | 11.1.3 | 2 | Missing | Validate minimum-rating-count threshold is enforced server-side |
| F-16 Note Reassignment | V4.3 | 4.3.1 | 2 | Missing | Admin role must be re-verified server-side on each reassignment POST |
| F-16 Note Reassignment | V7.1 | 7.1.2 | 2 | Missing | Log reassignment: admin actor, source owner, target owner, note ID, timestamp |
| F-16 Note Reassignment | V11.1 | 11.1.4 | 2 | Missing | Validate target user exists and is not the same as current owner before committing |

---

## Enhanced Feature Specifications

### Feature F-01: User Registration (REQ-001)

**Original Statement**: Users can register with a username, email address, and password.

**Augmented Requirement Statements**:
- Passwords must meet minimum length of 12 characters; system must not impose a maximum length below 128 characters
- Passwords must not be required to contain mixed-case, numerics, or symbols as mandatory composition rules (complexity rules are counterproductive per NIST SP 800-63B); length is the primary strength control
- The system should check submitted passwords against a configurable breach/common-password list (e.g., HIBP API or local blocklist) and reject matches
- Username and email must be validated server-side: email must be well-formed RFC 5321; username must meet defined character set and length constraints (e.g., 3ΓÇô50 alphanumeric + underscore)
- Registration form must include an anti-forgery token
- System must not expose whether an email address is already registered through differing response messages or timing (account enumeration prevention)
- Only collect: username, email, password. No additional PII collected at registration

**ASVS Mapping**: V2.1.1, V2.1.2, V2.1.5, V2.1.7, V2.1.9, V2.4.1, V5.1.3, V8.3.4, V14.4.1

**SSEM Implementation Notes**:
- **Analyzability**: Registration controller must have a single, clearly named validation method; password policy must be centralized in a `PasswordPolicyService`, not scattered inline
- **Modifiability**: Password policy (min length, blocklist source) must be externalized to configuration; changing policy must not require controller changes
- **Testability**: Provide unit tests for each password policy rule; provide integration tests for duplicate-email/username collision behavior; provide tests that verify anti-enumeration response parity
- **Confidentiality**: Never log submitted passwords or password hashes; PII (email) must be stored with access limited to the user and admins
- **Accountability**: Log registration events with timestamp and IP; log failed validation attempts without echoing the submitted value
- **Authenticity**: ASP.NET Identity PBKDF2 with iteration count meeting current OWASP guidance (ΓëÑ310,000 iterations for SHA-256) must be verified in configuration
- **Availability**: Registration endpoint must be rate-limited to prevent account-creation spam and resource exhaustion
- **Integrity**: User ID must be server-generated (not client-supplied); user record must be written atomically
- **Resilience**: If breach-list check service is unavailable, system must fail open with a log warning (registration proceeds) rather than blocking all registrations

**FIASSE Tenet Annotations**:
- S2.1: Password policy is expressed as configurable quality constraints, not a static rule set; breach-list integration allows the policy to adapt without code changes as new threat data emerges
- S2.2: Rate limiting on registration protects availability of the registration flow under enumeration or spam stress; breach-open behavior on blocklist failure preserves user value delivery
- S2.3: Anti-enumeration response parity reduces the attacker's ability to pre-harvest valid accounts, directly reducing the material impact of future credential attacks
- S2.4: Centralized `PasswordPolicyService` and blocklist integration are engineering controls that scale across the application; they replace ad-hoc per-form validation
- S2.6: Registration audit log (timestamp, IP, outcome) provides observable registration activity without logging sensitive credential material

**Acceptance Criteria**:
- [ ] Passwords shorter than 12 characters are rejected with a clear error message
- [ ] Passwords on the configured blocklist are rejected with a message that does not reveal the blocklist contents
- [ ] Submitting an email that already exists returns the same response/timing as submitting a new email
- [ ] Anti-forgery token validation failure returns HTTP 400
- [ ] Registration events appear in the structured audit log with outcome, IP, and timestamp
- [ ] Password is stored as PBKDF2 hash; no plaintext appears in any log or database field

---

### Feature F-02: User Login / Authentication (REQ-002)

**Original Statement**: Users log in using username and password; session persists until logout or timeout.

**Augmented Requirement Statements**:
- After 5 consecutive failed login attempts from a single account within a configurable time window, the account must be temporarily locked for a configurable duration (default: 15 minutes) or until admin unlock
- Lockout state must not reveal whether the account exists (maintain anti-enumeration parity in the response)
- Authentication cookie must be set with `Secure`, `HttpOnly`, and `SameSite=Lax` (minimum) attributes
- Session token must never appear in query strings, URLs, or log files
- Idle session timeout: 30 minutes (configurable)
- Absolute session timeout: 8 hours (configurable)
- On logout, the server-side session must be invalidated (not just the client cookie cleared)
- Successful login must rotate the session token to prevent session fixation
- Failed login attempts must be logged with timestamp and IP; no credential values logged

**ASVS Mapping**: V2.2.1, V2.2.2, V3.1.1, V3.3.1, V3.3.2, V3.3.4, V3.4.1, V3.4.2, V3.4.3, V3.4.4, V7.1.1, V7.1.2

**SSEM Implementation Notes**:
- **Analyzability**: Lockout logic must be isolated in a `LoginThrottleService`; authentication flow must be readable top-to-bottom without inlined retry-counting logic
- **Modifiability**: Lockout threshold, window, and duration must be in `appsettings.json` under a named `LoginPolicy` section; cookie attribute configuration must be centralized in `Program.cs` cookie options
- **Testability**: Unit tests must cover: lockout triggers on N+1 attempts, lockout reset after window, session cookie attribute presence, session invalidation on logout
- **Confidentiality**: Session tokens must have sufficient entropy (ΓëÑ128 bits); ASP.NET Data Protection API must be used for session cookie protection
- **Accountability**: Every authentication attempt (success and failure) must produce a structured log entry: event type, actor (username), IP, timestamp, outcome
- **Authenticity**: Session token rotation at login must be verified; reuse of pre-login session ID must be impossible
- **Availability**: Lockout mechanism must not be bypassable via concurrent request flooding; lockout state must be stored server-side (not client-side)
- **Integrity**: User identity in session must be bound to the server-issued identity claim; client must not be able to modify session-stored user ID
- **Resilience**: If the session store is temporarily unavailable, the system must return a user-friendly error and log the failure without leaking session data

**FIASSE Tenet Annotations**:
- S2.1: Lockout thresholds and session timeouts are configurable quality attributes, not hardcoded values; they can evolve with threat posture without code changes
- S2.2: Absolute session timeout ensures long-lived sessions cannot accumulate indefinitely; idle timeout preserves usability for legitimate users while constraining abandoned session exposure
- S2.3: Account lockout reduces the value of credential lists to attackers; session fixation prevention eliminates a whole class of session hijacking attacks
- S2.4: Centralized cookie configuration and throttle service are reusable, auditable engineering controls
- S2.6: Structured authentication event logs enable detection of brute-force patterns and provide a traceable audit trail for incident response

**Acceptance Criteria**:
- [ ] Account locked after 5 failed attempts within the configured window; response is identical to unknown-account response
- [ ] Authentication cookie contains `Secure`, `HttpOnly`, and `SameSite=Lax` attributes
- [ ] Session ID does not appear in any URL or server log
- [ ] New session ID is issued upon successful authentication
- [ ] Server-side session is destroyed on logout; replaying the old session cookie returns unauthenticated
- [ ] Idle timeout of 30 minutes and absolute timeout of 8 hours are enforced
- [ ] All login attempts appear in the structured audit log

---

### Feature F-03: Password Reset (REQ-003)

**Original Statement**: Users request reset by email; time-limited token (1 hour) allows new password; used/expired tokens rejected.

**Augmented Requirement Statements**:
- Reset tokens must be cryptographically random with at least 128 bits of entropy
- Tokens must be single-use: immediately invalidated after first use, whether successful or not
- Token expiry must be enforced server-side; client-supplied expiry claims must not be trusted
- The password reset confirmation response must be identical whether the email address is registered or not (anti-enumeration)
- New password set via reset must meet the same policy as registration (REQ-001 policy)
- After a successful password reset, all existing sessions for the account must be invalidated
- Reset requests must be rate-limited per email address and per IP to prevent email flooding
- Password reset events (request, completion, failure) must be logged with timestamp, IP, and user identity (not token value)

**ASVS Mapping**: V2.5.1, V2.5.2, V2.5.3, V2.5.4, V2.5.6, V7.1.1, V7.1.2

**SSEM Implementation Notes**:
- **Analyzability**: Token generation and validation must be encapsulated in a `PasswordResetTokenService`; no token logic dispersed across controllers
- **Modifiability**: Token expiry duration must be configuration-driven; token generation algorithm must be replaceable without controller changes
- **Testability**: Tests must cover: expired token rejection, double-use rejection, valid token acceptance, session invalidation post-reset, anti-enumeration response parity
- **Confidentiality**: Token values must never appear in server logs; email must only contain the token link, not the raw token value separately
- **Accountability**: All reset lifecycle events logged: request (email, IP, timestamp), completion (user ID, timestamp), failure (reason category, not token value)
- **Authenticity**: Token must be bound to the specific user account; cross-account token substitution must be impossible
- **Availability**: Rate limiting prevents email flooding while allowing legitimate resets; if email service is down, log the failure and surface a user-friendly error
- **Integrity**: Password written to the database on reset must be validated before storage; partial-write failure must not leave account in inconsistent state
- **Resilience**: If session invalidation fails after reset, the system must log a critical error and surface it for operator attention; the reset itself must not silently proceed without invalidation

**FIASSE Tenet Annotations**:
- S2.1: Token service design avoids baking in a specific algorithm or expiry; both are configuration-driven to adapt as standards evolve
- S2.2: Rate limiting on reset requests ensures the feature remains available for legitimate users even under abuse
- S2.3: Session invalidation post-reset eliminates hijacked-session persistence after account recovery; anti-enumeration prevents harvesting valid account emails
- S2.4: Single-use tokens and server-side expiry enforcement are engineering controls that close an entire class of token-replay attacks
- S2.6: Reset audit log provides traceability for account takeover investigations without exposing token material

**Acceptance Criteria**:
- [ ] Submitting a reset request for a nonexistent email returns the same response as for a valid email
- [ ] Reset token cannot be used twice; second use returns an error
- [ ] Token expires after 1 hour; post-expiry use returns an error
- [ ] All pre-reset sessions are invalidated upon successful password change
- [ ] New password must pass the same policy tests as registration
- [ ] Reset events appear in the structured audit log with actor, IP, and timestamp

---

### Feature F-04: Note Creation (REQ-004)

**Original Statement**: Authenticated users create notes with title and content, linked to creator's user ID, defaulting to private.

**Augmented Requirement Statements**:
- The note's `UserId` must be bound server-side from the authenticated session; it must not be accepted as a form input
- Title must be validated server-side: required, maximum 500 characters
- Content must be validated server-side: required, maximum configurable length (default: 100,000 characters)
- Output rendering of note content in all views must apply context-appropriate encoding (HTML encoding for browser display; no raw HTML injection unless a WYSIWYG policy is explicitly defined)
- Default visibility must be `Private`; any deviation from default must require explicit user action
- Creation timestamp must be set server-side; client-supplied timestamps must not be accepted
- Anti-forgery token required on the creation form

**ASVS Mapping**: V4.2.1, V5.1.3, V5.1.4, V5.3.1, V5.3.3, V7.1.2

**SSEM Implementation Notes**:
- **Analyzability**: Note creation logic must be in `NotesController.Create` with a corresponding `CreateNoteViewModel`; business rules (defaults, ownership binding) must be readable without tracing through multiple layers
- **Modifiability**: Content size limit must be in configuration; encoding strategy must be handled by a shared utility or Razor's built-in encoding, not per-view custom logic
- **Testability**: Tests must cover: UserId from session (not form), missing title rejection, oversized content rejection, default visibility = private, server-side timestamp
- **Confidentiality**: Note content defaults to private; no note content is exposed in API responses or logs beyond what the owner sees
- **Accountability**: Note creation events logged: user ID, note ID, visibility setting, timestamp
- **Authenticity**: UserId bound from `User.GetUserId()` (claims principal), not from a hidden form field
- **Availability**: Content length limits prevent unbounded database writes; anti-forgery token prevents CSRF-driven note creation
- **Integrity**: All fields written to the database are validated before persistence; partial-write atomicity ensured via EF transaction
- **Resilience**: Validation failures must return the form with preserved (safe) user input and clear error messages; no silent failures

**FIASSE Tenet Annotations**:
- S2.1: Server-side ownership binding ensures the security model remains correct regardless of future frontend changes
- S2.2: Size limits protect database and rendering performance under high load; validated defaults ensure expected behavior without requiring user awareness of the security model
- S2.3: Default private visibility minimizes the blast radius of users inadvertently exposing content; server-side ownership prevents privilege escalation through form manipulation
- S2.4: Razor's built-in HTML encoding is a systematic engineering control that prevents XSS at the output layer without per-field developer effort
- S2.6: Creation audit log provides provenance for each note; supports admin investigations of content disputes

**Acceptance Criteria**:
- [ ] Note creation with a forged `userId` in the form body creates the note under the authenticated user, not the forged ID
- [ ] Notes with missing title or content are rejected with validation errors
- [ ] Notes with title > 500 characters are rejected
- [ ] Newly created notes are `Private` by default without explicit user action
- [ ] Anti-forgery token validation failure returns HTTP 400
- [ ] Creation event appears in structured audit log

---

### Feature F-05: File Attachment (REQ-005)

**Original Statement**: Users upload files (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG); stored with unique IDs; original filename in metadata.

**Augmented Requirement Statements**:
- Maximum file size per upload: 10 MB (configurable); reject oversized files before reading content
- Maximum total storage per user: configurable quota (e.g., 500 MB); enforce before accepting upload
- File content must be validated by inspecting file magic bytes (file signature), not solely by the submitted file extension or MIME type
- Uploaded files must be stored outside the web root directory; they must not be directly accessible via URL path
- Stored filenames must be server-generated UUIDs with the validated extension appended; the original filename must only be stored in database metadata, never used as a filesystem path component
- File download must be served through a controller action that sets `Content-Disposition: attachment` and the validated content type
- Executable file types (EXE, DLL, SH, BAT, PS1, ASPX, PHP, etc.) must be explicitly blocked regardless of submitted MIME type
- File access must be authorization-checked: only the note owner, users with a valid share link for the note, or admins may download attachments
- Virus/malware scanning integration point must be defined in the architecture (implementation optional for v1, but the interface must be present)

**ASVS Mapping**: V12.1.1, V12.1.2, V12.1.3, V12.2.1, V12.3.1, V12.3.2, V12.3.3, V12.3.4, V12.4.1, V12.4.2, V12.5.1, V12.5.2

**SSEM Implementation Notes**:
- **Analyzability**: All file handling must be encapsulated in a `FileStorageService` implementing `IFileStorageService`; no file I/O in controllers
- **Modifiability**: Allowed extensions, blocked extensions, size limits, storage path, and quota must all be in a `FileStorageOptions` configuration section; scanner integration must be behind an `IFileScanService` interface
- **Testability**: Tests must cover: magic-byte validation rejects mismatched extension, oversized file rejection, path traversal attempt rejection, download authorization check, correct `Content-Disposition` header
- **Confidentiality**: Attachment access gated by note ownership or valid share token; file metadata (original name) must not leak to unauthorized users
- **Accountability**: File upload and download events logged: actor, note ID, file ID, original filename (sanitized), timestamp, outcome
- **Authenticity**: File access controller must re-verify that the requesting user is authorized for the parent note before serving the file
- **Availability**: File size and quota limits protect storage from exhaustion; rate limiting on uploads prevents upload-spam attacks
- **Integrity**: UUID-based filenames prevent path traversal; magic-byte validation prevents content-type spoofing; `Content-Disposition: attachment` prevents browser execution of served files
- **Resilience**: If file storage service is unavailable, upload must fail with a user-friendly error; note creation must not succeed with a dangling attachment reference; transactions must roll back atomically

**FIASSE Tenet Annotations**:
- S2.1: `IFileScanService` interface and configurable blocklist allow the security posture to evolve (e.g., adding AV scanning later) without redesigning the upload flow
- S2.2: Quota and size limits ensure upload availability is preserved for all users; graceful storage-failure error prevents silent data corruption
- S2.3: UUID filenames, out-of-web-root storage, and execution prevention collectively eliminate the most impactful file upload attack classes (path traversal, webshell upload, direct execution)
- S2.4: Magic-byte validation is a systematic engineering control applied uniformly at the service boundary; it cannot be bypassed by changing a file extension
- S2.6: Upload/download audit log enables detection of bulk exfiltration attempts and provides traceability for content disputes

**Acceptance Criteria**:
- [ ] A file with a `.jpg` extension but PDF magic bytes is rejected
- [ ] A file with an executable extension (`.exe`, `.aspx`, etc.) is rejected regardless of submitted MIME type
- [ ] A file exceeding the size limit is rejected before content is fully read
- [ ] The stored file path is a UUID-based name; original filename is not used in any file system path
- [ ] Direct URL access to the upload storage directory returns 404 or 403
- [ ] File download response contains `Content-Disposition: attachment` with the sanitized original filename
- [ ] Attempting to download another user's attachment without authorization returns 403
- [ ] Upload and download events appear in the structured audit log

---

### Feature F-06: Note Editing (REQ-006)

**Original Statement**: Note owners can modify title, content, and visibility; last-modified timestamp updated on save.

**Augmented Requirement Statements**:
- Server-side ownership verification must be performed on every edit request using the authenticated user's session identity; form-supplied note IDs must only identify the record, not grant ownership
- Changing note visibility from private to public must require explicit user acknowledgment (e.g., a confirmation step or clearly labeled toggle with current state displayed)
- Last-modified timestamp must be set server-side; client-supplied timestamps must be rejected
- Anti-forgery token required on the edit form
- The same content validation rules as note creation (title length, content length, encoding) apply to edits

**ASVS Mapping**: V4.2.1, V5.1.3, V5.3.1, V11.1.1, V11.1.2

**SSEM Implementation Notes**:
- **Analyzability**: Ownership check must be a clearly named method call at the start of the edit action, not buried in a query filter
- **Modifiability**: Visibility change confirmation behavior must be configurable; content validation rules must be shared with the creation flow (single policy definition)
- **Testability**: Tests must cover: editing another user's note returns 403, visibility change confirmation skipped does not publish note, server-side timestamp is set correctly
- **Confidentiality**: Edit form must not pre-populate the note ID in a way that leaks other notes' IDs to the client
- **Accountability**: Note edit events logged: user ID, note ID, fields changed (not values), timestamp, old visibility ΓåÆ new visibility if changed
- **Authenticity**: Ownership claim is derived from session, not from a form parameter; concurrent-edit conflicts must be detected (ETag or rowversion)
- **Availability**: Edit form must be protected against CSRF; large content edits must respect the same size limits as creation
- **Integrity**: Visibility field must be validated as a boolean/enum; arbitrary string values must be rejected; partial save failures must not leave note in inconsistent state
- **Resilience**: If database update fails, user must see a clear error and the form must retain their edits; no silent data loss

**FIASSE Tenet Annotations**:
- S2.1: Centralized validation and ownership checks mean the editing security model holds regardless of future UI changes
- S2.2: Visibility confirmation prevents accidental data exposure under stress or fast-click scenarios
- S2.3: Server-side ownership enforcement eliminates IDOR vulnerability on edit; this is the highest-impact control for this feature
- S2.4: Shared validation rules across creation and editing are an engineering control that prevents inconsistency-driven bypass
- S2.6: Change audit log (old visibility ΓåÆ new) provides a traceable history for content governance and dispute resolution

**Acceptance Criteria**:
- [ ] `PUT /notes/{id}` with a valid session for a different user returns 403
- [ ] Changing visibility to public without confirmation step is blocked by form validation
- [ ] Last-modified timestamp in the database matches server time, not client-submitted time
- [ ] Anti-forgery token failure returns HTTP 400
- [ ] Note edit events (including visibility changes) appear in the structured audit log

---

### Feature F-07: Note Deletion (REQ-007)

**Original Statement**: Owner or admin can delete notes; confirmation prompt shown; cascading deletion of attachments, ratings, and share links.

**Augmented Requirement Statements**:
- Deletion must be confirmed via a POST request with anti-forgery token; GET requests must not trigger deletion
- Server-side authorization must verify the requesting user is the note owner or has the Admin role before proceeding; the check must use session identity, not form parameters
- Cascading deletion of attachments, ratings, and share links must be atomic (single transaction); partial deletion must roll back
- Physical file deletion for attachments must be performed after the database transaction commits to avoid orphaned files on rollback
- Deletion must be rate-limited to prevent rapid bulk deletion (anti-automation)
- Deletion events must be logged: actor, note ID, number of cascaded records, timestamp

**ASVS Mapping**: V4.2.1, V5.4.1, V7.1.2, V11.1.4

**SSEM Implementation Notes**:
- **Analyzability**: Deletion authorization check and cascading logic must be clearly separated; a single `NoteService.DeleteAsync` method should orchestrate the sequence
- **Modifiability**: Cascading delete behavior must be defined in EF model configuration (not scattered controller logic); file deletion post-commit must be isolatable for testing
- **Testability**: Tests must cover: non-owner returns 403, admin deletion succeeds, partial cascade failure rolls back, physical files are removed after successful database deletion
- **Confidentiality**: Deleted note content must not appear in any log entry
- **Accountability**: Deletion event log captures: actor identity, note ID, cascaded record counts, timestamp
- **Authenticity**: Authorization derived from session claims; Admin role claim must be verified via ASP.NET Identity, not a client-supplied flag
- **Availability**: Rate limit prevents a single actor from bulk-deleting their notes in a way that stresses the database
- **Integrity**: Transaction wraps all database deletes; file deletion outside the transaction is post-commit only
- **Resilience**: If file deletion fails post-commit, log the orphaned file reference for background cleanup; do not re-expose the note

**FIASSE Tenet Annotations**:
- S2.1: Atomic transactional deletion with post-commit file cleanup is a design pattern that remains correct across future schema changes
- S2.2: Rate limiting ensures deletion service remains responsive; atomic rollback ensures data integrity is not partially degraded under failure
- S2.3: Cascading deletion of share links ensures that deleted note content is no longer accessible via any path; this directly limits post-deletion exposure
- S2.4: POST-only deletion with anti-forgery token is an engineering control against CSRF-driven deletion
- S2.6: Deletion audit log provides the evidence needed to investigate accidental or malicious bulk content deletion

**Acceptance Criteria**:
- [ ] `GET /notes/{id}/delete` does not delete; only `POST /notes/{id}/delete` with valid anti-forgery token deletes
- [ ] Non-owner attempting to delete another user's note receives 403
- [ ] If a cascade deletion of ratings fails, the note and all related records remain intact (transaction rolled back)
- [ ] Share links for a deleted note are no longer resolvable after deletion
- [ ] Deletion event appears in the structured audit log with cascaded record counts

---

### Feature F-08: Note Sharing (REQ-008)

**Original Statement**: Owners generate unique share links; anyone with link can view note and attachments without authentication; links can be regenerated/revoked.

**Augmented Requirement Statements**:
- Share tokens must be generated using a cryptographically secure random generator with at least 128 bits of entropy
- Share link validation must be performed server-side on every request; client-side-only token checks are insufficient
- Share link access must not expose any metadata beyond the note's content and its attachments: no internal note IDs, user IDs, or database keys must be surfaced in the share view
- Share link access sessions must not be upgradeable to authenticated sessions; unauthenticated share view must not create a session cookie
- Share link generation must be rate-limited per user (e.g., maximum 20 active share links per user)
- Revoked or regenerated share links must be immediately invalid server-side; old tokens must not be cached or reused
- Share view must not allow rating, editing, or any state-changing operation
- Share link access events must be logged: token ID (not token value), IP, timestamp

**ASVS Mapping**: V3.5.2, V3.5.3, V4.1.3, V4.2.1, V8.3.1, V11.1.6, V7.1.2

**SSEM Implementation Notes**:
- **Analyzability**: `ShareTokenService` must clearly separate token generation, validation, and revocation; share view controller must be clearly read-only
- **Modifiability**: Token entropy length, active link limit, and expiry policy must be in configuration; token generation algorithm must be behind an interface
- **Testability**: Tests must cover: revoked token access returns 404/410, regenerated old token is invalid, rate limit blocks excess link creation, share view contains no authenticated-user metadata
- **Confidentiality**: Share view must render only title, content, and attachments; internal IDs must not appear in DOM or API responses; no auth cookies issued to share viewers
- **Accountability**: Share link creation, access, and revocation events logged with token ID (not value), actor (for creation/revocation), IP, timestamp
- **Authenticity**: Token looked up by value in the database; revocation status checked at lookup time; no token validity can be derived from the token value alone (opaque token)
- **Availability**: Rate limiting on link generation prevents link-spam; efficient token lookup must use a database index on the token value column
- **Integrity**: Share view must be a read-only surface; all state-changing operations must require full authentication regardless of share token presence
- **Resilience**: If token lookup fails transiently, return a user-friendly error; do not fall through to an authenticated session fallback

**FIASSE Tenet Annotations**:
- S2.1: Opaque server-side tokens allow token format and entropy to evolve without invalidating the sharing architecture
- S2.2: Rate limiting and immediate revocation enforcement ensure share links remain a reliable feature without enabling abuse
- S2.3: Metadata suppression in share view limits what an attacker can learn about the system's internal structure from an unauthenticated surface
- S2.4: Server-side token validation on every request (not cached) is an engineering control that ensures revocation takes effect immediately
- S2.6: Token access logs (using token ID, not token value) provide traceability for unauthorized content access investigations

**Acceptance Criteria**:
- [ ] Share token has at least 128 bits of entropy (verified by code review of generation method)
- [ ] Revoking a share link makes it immediately inaccessible (returns 404 within the same request cycle)
- [ ] Share view page contains no internal user IDs, note IDs, or database keys in DOM source
- [ ] No session cookie is set for unauthenticated share view access
- [ ] Creating more than 20 active share links returns an error
- [ ] Share access events appear in the audit log by token ID and IP

---

### Feature F-09: Public/Private Notes (REQ-009)

**Original Statement**: Notes have public/private toggle; public notes appear in search and are viewable by anyone; private notes visible only to owner.

**Augmented Requirement Statements**:
- Visibility enforcement must be applied server-side on every query; client-side-only visibility controls are insufficient
- Any API or search endpoint must explicitly filter private notes from non-owner results using a parameterized WHERE clause; the filter must not rely on result-set post-processing
- The visibility field in the database must be an enumeration (not a free-text field); values outside the defined set must be rejected
- Changing a note from private to public must be logged as a notable audit event

**ASVS Mapping**: V4.1.3, V4.2.1, V5.1.1, V5.3.4

**SSEM Implementation Notes**:
- **Analyzability**: Visibility filter logic must be in a shared query extension method (e.g., `VisibleToUser(userId)`) applied consistently across all note-retrieving queries
- **Modifiability**: Future addition of "shared with specific users" visibility level must be achievable by extending the enum and the filter method, not by modifying every controller
- **Testability**: Tests must cover: private note query returns empty for non-owner, public note returns for all users, direct URL access to private note returns 403 for non-owner
- **Confidentiality**: Private note content must not appear in any API response, search result, or error message visible to non-owners
- **Accountability**: Visibility changes logged (private ΓåÆ public is a significant event)
- **Authenticity**: Visibility check uses authenticated user ID from session; unauthenticated requests can only see public notes
- **Availability**: Visibility filter applied at database query level (not application level) to keep query performance bounded
- **Integrity**: Visibility stored as a typed enum; invalid values rejected at model binding and database constraint levels
- **Resilience**: If visibility filter clause is removed or misconfigured, the absence of a filter must be detectable via tests; defense-in-depth through deny-by-default query design

**FIASSE Tenet Annotations**:
- S2.1: Shared `VisibleToUser` filter method means visibility logic is defined once and enforced consistently as new query paths are added
- S2.2: Database-level filtering preserves query performance under growing data volumes
- S2.3: Deny-by-default query design (start with private, add public where appropriate) minimizes unintended data exposure
- S2.4: Typed enum for visibility prevents injection of unexpected values; centralized filter prevents inconsistent per-endpoint enforcement
- S2.6: Visibility change audit events provide a record for investigating unexpected content exposure

**Acceptance Criteria**:
- [ ] Querying notes as user B does not return user A's private notes
- [ ] Directly accessing the URL of a private note as a non-owner returns 403
- [ ] A note visibility field set to an unrecognized string value is rejected at model binding
- [ ] Private ΓåÆ public transition appears in the audit log

---

### Feature F-10: Note Rating (REQ-010)

**Original Statement**: Authenticated users rate notes with 1-5 stars and optional comment; users can edit their own rating.

**Augmented Requirement Statements**:
- Rating value must be validated server-side as an integer in the range [1, 5]; out-of-range values must be rejected
- Comment text must be validated: maximum length (e.g., 1,000 characters); prohibited character sets or markup must be enforced if comments are rendered as HTML
- Rating ownership must be verified server-side before allowing edits; a user may only edit their own rating
- A user may submit at most one rating per note; duplicate rating attempts must be rejected with a meaningful error
- Rating submission must be rate-limited to prevent spam rating
- Anti-forgery token required on rating forms

**ASVS Mapping**: V4.2.1, V5.1.3, V5.1.4, V5.3.1, V11.1.1, V11.1.3

**SSEM Implementation Notes**:
- **Analyzability**: Rating validation (range, uniqueness) must be in `RatingService`; controller must delegate to service
- **Modifiability**: Rating range and comment length limits in configuration; adding additional rating dimensions in the future must not require rewriting validation logic
- **Testability**: Tests must cover: out-of-range value rejection, duplicate rating rejection, editing another user's rating returns 403, comment length enforcement
- **Confidentiality**: Rater usernames visible to note viewers is an explicit design decision; ensure it is documented as intentional and acceptable
- **Accountability**: Rating submission and edit events logged: actor, note ID, rating value (not comment content in logs), timestamp
- **Authenticity**: Rating ownership verified from session; edit form must not accept a different `ratingId` than what belongs to the authenticated user
- **Availability**: Rate limiting on submission prevents automated rating manipulation
- **Integrity**: Average rating recalculation must be atomic with the new rating insert/update to prevent stale averages
- **Resilience**: Duplicate rating detection must use a database unique constraint, not only application-layer logic, to prevent race-condition duplicates

**FIASSE Tenet Annotations**:
- S2.1: Database unique constraint (not just application logic) for one-rating-per-user-per-note is a durable integrity control that survives future code changes
- S2.2: Rate limiting preserves rating feature availability and data quality under automated abuse
- S2.3: Ownership enforcement on edit closes a direct privilege escalation path; input validation prevents injection through comment fields
- S2.4: Average-calculation atomicity is an engineering control that prevents data integrity drift under concurrent operations
- S2.6: Rating event log provides evidence for investigating coordinated rating manipulation

**Acceptance Criteria**:
- [ ] Rating value of 0 or 6 is rejected with HTTP 400
- [ ] Submitting a second rating for the same note returns an error indicating a rating already exists
- [ ] Editing another user's rating returns 403
- [ ] Comment exceeding 1,000 characters is rejected
- [ ] Anti-forgery token failure returns HTTP 400
- [ ] Rating events appear in the structured audit log

---

### Feature F-11: Rating Management (REQ-011)

**Original Statement**: Note owners view all ratings including value, comment, username, timestamp; average calculated; sorted newest first.

**Augmented Requirement Statements**:
- The rating list endpoint must verify that the requesting user is the note owner (or admin) before returning the full rating list
- Average rating must be calculated in the database query (not in-memory across paginated results) to ensure consistency
- Rating list must support pagination to prevent unbounded result sets

**ASVS Mapping**: V4.2.1, V5.1.3

**SSEM Implementation Notes**:
- **Analyzability**: Rating retrieval logic in `RatingService.GetRatingsForNoteAsync`; ownership check at controller entry, before service call
- **Modifiability**: Pagination size configurable; sort order changeable without schema changes
- **Testability**: Tests must cover: non-owner access returns 403, paginated results are consistent, average matches manual calculation from test data
- **Confidentiality**: Full rating list (including commenter usernames) visible only to note owner and admins; public note view shows aggregated rating only
- **Accountability**: Access to full rating list logged for admin access; owner access is standard and need not be individually logged unless policy requires it
- **Authenticity**: Ownership check uses session identity; no bypass via query parameter
- **Availability**: Pagination prevents memory exhaustion on notes with thousands of ratings
- **Integrity**: Average displayed must be computed from the same dataset as the list (no caching skew)
- **Resilience**: If pagination parameters are out of range, return the first page rather than an error

**FIASSE Tenet Annotations**:
- S2.1: Paginated design scales as note popularity grows; no architectural refactor needed
- S2.2: Pagination preserves page load performance; database-side average prevents stale data
- S2.3: Access control on full rating list prevents non-owners from harvesting rater identities
- S2.4: Ownership check at the controller boundary is a systematic gate, not a per-item check
- S2.6: Admin access to rating lists logged for accountability

**Acceptance Criteria**:
- [ ] Non-owner requesting the full rating list returns 403
- [ ] Rating list is paginated (default page size defined in configuration)
- [ ] Displayed average matches the computed average from test seed data

---

### Feature F-12: Note Search (REQ-012)

**Original Statement**: Search by keywords matching title or content; returns owned notes (any visibility) and public notes; excludes others' private notes.

**Augmented Requirement Statements**:
- Search query must be validated server-side: maximum length (e.g., 500 characters); minimum length (e.g., 2 characters) to prevent trivial full-table scans
- Full-text search must use parameterized queries via EF Core; no string interpolation into query expressions
- Search results must not include any private notes belonging to other users; this filter must be applied in the database query, not post-query
- Search result excerpts must be HTML-encoded before rendering; no raw content injection into the results view
- Search endpoint must be rate-limited to prevent scraping

**ASVS Mapping**: V4.2.1, V5.1.3, V5.3.3, V5.3.4

**SSEM Implementation Notes**:
- **Analyzability**: Search query construction in `NoteService.SearchAsync`; visibility filter reused from F-09 shared filter method
- **Modifiability**: Search index strategy (EF LIKE vs. full-text index) must be isolatable behind a service interface; switching strategies must not affect controller or view
- **Testability**: Tests must cover: private notes of other users excluded from results, search query injection attempt is parameterized (no SQL injection), empty/short query rejected
- **Confidentiality**: Result excerpts must be truncated at defined length (200 chars per PRD); no metadata beyond title, excerpt, author, date returned
- **Accountability**: High-volume search patterns (e.g., >50 queries/minute per user) flagged in logs
- **Authenticity**: User identity from session used to determine which private notes are visible; unauthenticated search shows public notes only
- **Availability**: Query length limits and rate limiting protect database from expensive open-ended searches
- **Integrity**: ORM parameterization ensures search terms are treated as data, not SQL; LIKE wildcards automatically escaped by EF
- **Resilience**: If search index is degraded, return empty results with a user-facing notice rather than an unhandled exception

**FIASSE Tenet Annotations**:
- S2.1: Service-layer abstraction for search allows index strategy to evolve without exposing the change to the controller layer
- S2.2: Rate limiting and query length limits protect search availability under load
- S2.3: Visibility filter at query level prevents private content exposure through search; parameterized queries prevent SQL injection as a systematic control
- S2.4: Shared visibility filter (from F-09) applied consistently is an engineering control against per-endpoint inconsistency
- S2.6: High-frequency search logging supports detection of content-scraping behavior

**Acceptance Criteria**:
- [ ] Search for a term matching another user's private note returns no results for non-owner
- [ ] Search query of 1 character is rejected with a validation error
- [ ] Search query containing SQL injection characters is safely parameterized (verified by ORM query log)
- [ ] Search results render content as plain text with HTML encoding; no injected HTML is executed
- [ ] Rate limit blocks requests exceeding the configured threshold per user per time window

---

### Feature F-13: Admin Dashboard (REQ-013)

**Original Statement**: Admins view user count, note count, activity log; list users; search users by username/email.

**Augmented Requirement Statements**:
- Every admin endpoint (not only the dashboard landing page) must independently verify the Admin role via server-side authorization; no route-level-only protection
- Admin dashboard access must be logged on every access: actor, timestamp, IP
- User search by email or username must use parameterized queries
- User list must be paginated; unbounded user enumeration must not be possible in a single request
- The activity log shown must not expose sensitive data (password hashes, tokens, reset links)
- Admin access control failures (unauthorized access attempts) must be logged and must not return information about the existence of the admin panel

**ASVS Mapping**: V4.1.5, V4.3.1, V4.3.2, V5.3.4, V7.1.1, V7.1.2, V7.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Admin authorization enforced via a dedicated `[Authorize(Roles = "Admin")]` attribute applied to the entire `AdminController`; no per-action inline role checks
- **Modifiability**: Admin role name must be a named constant, not a magic string in attributes; adding new admin features must only require adding actions to the controller
- **Testability**: Tests must cover: non-admin user accessing any admin endpoint returns 403, admin access appears in audit log, user search parameterization, paginated user list
- **Confidentiality**: Activity log must exclude sensitive fields; user list must not expose password hash columns in any view or API response
- **Accountability**: Every admin action (view, search, access) must produce a structured audit log entry
- **Authenticity**: Admin role verified from the ASP.NET Identity claims principal on every request; impersonation or role escalation attempts must be detectable via logs
- **Availability**: User list pagination prevents memory and database exhaustion on large user bases
- **Integrity**: User search must return only matching users; no wildcard-all bypass must be possible via crafted query string
- **Resilience**: If audit log write fails, the admin action must still complete but the failure must be flagged as a critical error to operators

**FIASSE Tenet Annotations**:
- S2.1: Controller-level role attribute ensures all future admin actions are protected by default; no per-action protection gaps can emerge
- S2.2: Pagination ensures admin dashboard remains responsive as the user base grows
- S2.3: Access control failure logging enables detection of privilege escalation attempts; sensitive field exclusion from logs limits credential exposure
- S2.4: Controller-wide `[Authorize]` is a systematic engineering control that applies uniformly without per-action decisions
- S2.6: Comprehensive admin audit trail (every access, every action) is essential for detecting and investigating admin account compromise

**Acceptance Criteria**:
- [ ] Authenticated non-admin user accessing any `/admin/*` route receives 403
- [ ] Admin dashboard access appears in audit log with actor, IP, and timestamp
- [ ] Admin user list is paginated; requesting `/admin/users` without pagination parameters returns page 1 only
- [ ] User search query is parameterized (verified via EF query log); SQL injection attempt returns empty results, not an error stack trace
- [ ] Activity log display does not contain password hashes, token values, or reset URLs

---

### Feature F-14: User Profile Management (REQ-014)

**Original Statement**: Users update username, email, and password; changes saved.

**Augmented Requirement Statements**:
- Password change must require re-entry of the current password before accepting a new password
- Email or username change must require password re-verification (or a re-authentication challenge) before committing the change
- New password must satisfy the same policy as registration (REQ-001)
- Email change must trigger a verification email to the new address before the change is committed to prevent account takeover via email swap
- Profile change events (username, email, password) must be logged with actor identity and timestamp; changed values must not be logged (log event type only)
- Anti-forgery token required on all profile edit forms

**ASVS Mapping**: V2.1.1, V2.1.5, V2.6.1, V7.1.2, V5.1.3

**SSEM Implementation Notes**:
- **Analyzability**: Profile update logic split into discrete service methods: `ChangePasswordAsync`, `ChangeEmailAsync`, `ChangeUsernameAsync`; each with its own re-auth check
- **Modifiability**: Re-authentication requirement (current password vs. re-login challenge) configurable per change type
- **Testability**: Tests must cover: password change without current password fails, email change without re-auth fails, new password failing policy is rejected, event logged without values
- **Confidentiality**: New email address not committed until verification; old credentials must not be logged
- **Accountability**: Profile change audit log: actor, change type (not old/new values), timestamp
- **Authenticity**: Re-auth check uses the same password verification path as login; not a separate, potentially weaker path
- **Availability**: Anti-forgery token and rate limiting on profile changes prevent CSRF-driven profile hijacking
- **Integrity**: Email change via verification link ensures the new email is deliverable and under user's control before committing
- **Resilience**: If the verification email fails to send, the email change must not be committed; user must be notified to retry

**FIASSE Tenet Annotations**:
- S2.1: Verification-before-commit for email change is an adaptive control; if email verification requirements evolve, only the verification service needs updating
- S2.2: Re-auth requirement ensures profile changes remain reliable security controls under session compromise scenarios
- S2.3: Current-password requirement for sensitive changes directly limits the blast radius of session hijacking; attacker with a stolen session still cannot change credentials without the password
- S2.4: Discrete service methods per change type are an engineering control ensuring each change type has the appropriate safeguards applied systematically
- S2.6: Change type logged without values provides accountability without exposing credential material in logs

**Acceptance Criteria**:
- [ ] Password change without current password field populated is rejected
- [ ] Email change requires password re-verification before any database update
- [ ] New password failing the policy (e.g., <12 characters) is rejected on profile update
- [ ] Email change is not committed until the verification link in the new email is clicked
- [ ] Profile change events appear in audit log with change type and timestamp; no old or new values logged

---

### Feature F-15: Top Rated Notes (REQ-015)

**Original Statement**: Public notes sorted by average rating (descending); minimum 3 ratings to appear; shows title, author, average, count, preview.

**Augmented Requirement Statements**:
- The minimum-rating-count threshold (3) must be enforced in the database query, not as a post-query filter, to ensure consistent and efficient enforcement
- Only public notes must appear in the top-rated list; private notes must be excluded even if they have high ratings
- Preview excerpts (first 200 characters) must be HTML-encoded before rendering
- Top-rated results may be cached with a defined TTL (e.g., 5 minutes) to reduce database load; cache must not serve private notes to public users under any invalidation scenario

**ASVS Mapping**: V4.2.1, V5.3.1, V11.1.3

**SSEM Implementation Notes**:
- **Analyzability**: Top-rated query encapsulated in `NoteService.GetTopRatedAsync(int minRatings, int pageSize)`; visibility and minimum-rating filters applied in one query
- **Modifiability**: Minimum rating threshold and page size configurable; cache TTL configurable; cache can be disabled for testing
- **Testability**: Tests must cover: notes with fewer than 3 ratings excluded, private notes excluded, excerpt truncated at 200 characters and HTML-encoded
- **Confidentiality**: No private note data appears in response; author display name must be the display name, not the internal user ID
- **Accountability**: No specific audit requirement for viewing top-rated list (public feature)
- **Authenticity**: Query does not require authentication; public endpoint
- **Availability**: Caching reduces database load for a high-traffic endpoint; pagination prevents unbounded response sizes
- **Integrity**: Minimum-rating filter in query prevents statistical manipulation via freshly created accounts with a single high rating
- **Resilience**: If cache is unavailable, fall back to live query; do not return stale or empty results silently

**FIASSE Tenet Annotations**:
- S2.1: Configurable threshold means the minimum-rating policy can evolve without code changes
- S2.2: Caching with fallback ensures the top-rated page remains available even under database load spikes
- S2.3: Database-level visibility and minimum-rating filters remove manipulation attack surface; cache invalidation design prevents private data bleeding into public cache
- S2.4: Encapsulated query method with both filters applied together is an engineering control that prevents partial-filter bypass
- S2.6: No specific audit needed for a public browse feature; note that cache hit/miss metrics should be observable via application metrics

**Acceptance Criteria**:
- [ ] A public note with 2 ratings does not appear in the top-rated list
- [ ] A private note with 5 ratings does not appear in the top-rated list
- [ ] Preview text is truncated at 200 characters and HTML-encoded in the rendered view
- [ ] Cache TTL and minimum-rating threshold values are present in `appsettings.json`

---

### Feature F-16: Note Ownership Reassignment (REQ-016)

**Original Statement**: Admin can change the owner of any note to a different existing user.

**Augmented Requirement Statements**:
- Admin role must be re-verified server-side on the reassignment POST action; controller-level attribute alone is not sufficient for a destructive admin operation
- Target user must be validated to exist in the database before committing the reassignment
- Target user must differ from the current owner; self-reassignment must be rejected
- Reassignment must be logged with full context: admin actor, original owner ID, target owner ID, note ID, timestamp
- A confirmation step must be required before committing reassignment to prevent accidental reassignment
- Reassignment must be atomic; partial success must roll back

**ASVS Mapping**: V4.3.1, V4.3.2, V7.1.2, V11.1.4

**SSEM Implementation Notes**:
- **Analyzability**: Reassignment logic in `AdminService.ReassignNoteAsync`; admin controller delegates entirely to service; authorization check explicit at controller and service boundary
- **Modifiability**: Adding audit fields or notification behavior on reassignment must not require controller changes
- **Testability**: Tests must cover: non-admin access returns 403, nonexistent target user returns validation error, self-reassignment rejected, successful reassignment produces audit log entry
- **Confidentiality**: Reassignment confirmation page must show note title and user display names, not internal IDs
- **Accountability**: Full reassignment audit log is a compliance requirement; log must be append-only (no modification or deletion of reassignment records)
- **Authenticity**: Admin identity re-verified from session claims on POST; cannot be bypassed by manipulating the confirmation form
- **Availability**: No specific rate limit needed for reassignment (admin-only, low frequency); but admin panel overall should be monitored
- **Integrity**: Atomic transaction prevents note from entering a state with no valid owner; foreign key constraint ensures target user exists
- **Resilience**: If the reassignment fails mid-transaction, the note must remain with its original owner; no dangling ownership state

**FIASSE Tenet Annotations**:
- S2.1: Service-layer reassignment method means the operation's integrity controls are durable across future admin UI changes
- S2.2: Atomic transaction ensures no partial state; confirmation step prevents accidental misuse of a powerful admin capability
- S2.3: Full audit log for reassignment directly reduces the material impact of admin account compromise by providing complete traceability
- S2.4: Dual authorization check (controller attribute + service-layer check) is defense-in-depth engineering, not a hack
- S2.6: Append-only audit trail for reassignment events is a key observability control for governance and compliance

**Acceptance Criteria**:
- [ ] `POST /admin/reassign` from a non-admin session returns 403
- [ ] Reassignment to a nonexistent user ID returns a validation error
- [ ] Reassignment to the current owner returns a validation error
- [ ] Reassignment requires a confirmation step (two-step POST flow with anti-forgery token)
- [ ] Successful reassignment event appears in the audit log with actor, source owner, target owner, note ID, and timestamp
- [ ] If the database transaction fails, the note remains with its original owner

---

## Global Securability Requirements

These cross-cutting requirements apply to the entire application and are not specific to a single feature.

### GR-01: HTTP Security Headers

- `Content-Security-Policy` must be configured to restrict script, style, and frame sources; inline scripts must be disallowed or require a nonce
- `X-Content-Type-Options: nosniff` must be set on all responses
- `X-Frame-Options: DENY` (or `SAMEORIGIN` if embedding is required) must be set
- `Referrer-Policy: strict-origin-when-cross-origin` must be set
- `Strict-Transport-Security` (HSTS) must be configured with `max-age` ΓëÑ 31536000 and `includeSubDomains` for production deployments

**ASVS Mapping**: V14.4.1ΓÇôV14.4.7

### GR-02: Dependency and Build Security

- All third-party NuGet packages must be sourced from a verified feed; `NuGet.Config` must specify allowed sources
- Known-vulnerable packages must be identified via automated scanning (e.g., `dotnet list package --vulnerable`) as part of the build pipeline
- Build must produce a software bill of materials (SBOM) or equivalent dependency manifest

**ASVS Mapping**: V14.2.1, V14.2.2, V14.2.5

### GR-03: Structured Logging

- All audit-relevant events must be logged as structured JSON with fields: `timestamp`, `event_type`, `actor_id`, `ip_address`, `outcome`, `resource_id` (where applicable)
- Log output must not contain: passwords, password hashes, session tokens, share token values, or reset token values
- Application error logs must include a correlation ID traceable through the request lifecycle
- Log storage must be separated from application storage; log files must not be accessible via the web root

**ASVS Mapping**: V7.1.1, V7.1.2, V7.2.1, V7.2.2, V7.3.1, V7.4.1

### GR-04: Error Handling

- Unhandled exceptions must return a generic error page with a correlation ID; no stack traces, exception types, or internal paths must be exposed to users
- HTTP 404 responses must not reveal whether a resource exists but is forbidden; use HTTP 403 only when the user is authenticated and known to lack access
- Developer exception pages must be disabled in production; `appsettings.Development.json` settings must not be deployed to production

**ASVS Mapping**: V7.4.1, V7.4.2, V14.3.1, V14.3.2, V14.3.3

### GR-05: Transport Security

- All HTTP traffic must be redirected to HTTPS
- TLS version must be 1.2 minimum; TLS 1.3 preferred
- No sensitive data may be transmitted in URL query parameters

**ASVS Mapping**: V9.1.1, V9.1.2, V9.1.3

### GR-06: Anti-CSRF

- All state-changing operations (POST, PUT, DELETE) must include and validate anti-forgery tokens using ASP.NET Core's `[ValidateAntiForgeryToken]` attribute or global middleware
- AJAX state-changing requests must include the anti-forgery token in a request header

**ASVS Mapping**: V4.2.2, V13.2.3

### GR-07: Session Management

- ASP.NET Data Protection API must be used for session cookie encryption and anti-forgery token generation
- Data Protection keys must be stored with appropriate access controls (not in the web root); key rotation policy must be defined

**ASVS Mapping**: V3.4.1ΓÇôV3.4.5, V6.4.1

---

## Open Gaps and Assumptions

| ID | Gap / Assumption | Risk Level | Recommended Action |
|----|-----------------|------------|-------------------|
| GAP-01 | No multi-factor authentication (MFA) defined in PRD | Medium | Define MFA optionality for v2; document as a known gap with accepted risk for v1 |
| GAP-02 | Breach password list source not specified (HIBP API vs. local blocklist) | Medium | Select and document the blocklist strategy before implementation; HIBP k-anonymity API is recommended |
| GAP-03 | Email verification for registration not mentioned | Medium | Clarify whether email verification is required at registration or deferred; unverified accounts increase account-takeover risk |
| GAP-04 | Virus/malware scanning for uploads is an optional interface stub | High | For any production deployment: define SLA for scan integration; document accepted risk if deferred |
| GAP-05 | Data retention and deletion policy not defined | Medium | Define how long notes, ratings, and activity logs are retained; required for GDPR-adjacent compliance |
| GAP-06 | Share link expiry not defined | Medium | Consider adding optional expiry date to share links; current design is indefinite unless manually revoked |
| GAP-07 | No account deletion / right-to-erasure feature defined | Medium | Define user account deletion behavior; required for GDPR compliance in EU deployments |
| GAP-08 | Admin audit log is described as viewable in the admin dashboard; append-only guarantees not specified | High | Audit logs must not be mutable by admin users; define separate log store or database-level append-only enforcement |
| GAP-09 | Cloud storage integration for file attachments is mentioned but not specified | Low | If cloud storage (e.g., Azure Blob, S3) is used, define: signed URL policy, bucket access controls, and server-side encryption requirements |
| GAP-10 | Note content format (plaintext vs. Markdown vs. HTML) is undefined | High | If Markdown or HTML is supported, a sanitization library (e.g., HtmlSanitizer) must be specified and applied before storage and rendering; ASVS V5.3.1 and V5.2.1 apply |
| GAP-11 | Password policy is described as "minimum requirements" only; specific complexity policy not enumerated | Medium | Define explicit policy aligned to NIST SP 800-63B: length ΓëÑ12, no mandatory complexity, breach-list check; document in `PasswordPolicyOptions` configuration |
| GAP-12 | Attachment access control for share link viewers not fully specified | Medium | Confirm: do share link viewers have access to all attachments of the shared note? Define the authorization model explicitly |

---

## Conclusion

This enhanced PRD establishes securability requirements as first-class engineering constraints across all sixteen features of the Loose Notes Web Application. The ASVS Level 2 baseline provides a tested, production-appropriate security posture without over-engineering for risks not present in the stated deployment context.

Delivery teams should treat the per-feature acceptance criteria as part of the Definition of Done. Global requirements (GR-01 through GR-07) must be implemented as cross-cutting infrastructure before feature delivery begins, not retrofitted after. The twelve open gaps documented above represent the most important pre-implementation decisions required to avoid costly late-stage rework.
