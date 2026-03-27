# Project Requirements Document (Securability-Enhanced) ΓÇö Loose Notes Web Application

**Enhancement Framework**: FIASSE/SSEM + OWASP ASVS 5.0
**Enhancement Date**: 2026-03-26
**Baseline ASVS Level**: 2

---

## 0. ASVS Level Decision

### Selected Level: **2 ΓÇö Standard Production**

**Rationale:**

The Loose Notes application is a multi-user production web application that stores personal data (usernames, email addresses), manages user-generated content with visibility controls, accepts file uploads, and exposes an administrative privilege tier. These characteristics exceed the Level 1 baseline because:

- Authenticated user sessions govern access to owned content; session compromise directly enables unauthorized data access.
- Public/private visibility controls require reliable authorization enforcement; failure exposes private notes to unauthenticated parties.
- File upload is an inherently high-risk surface requiring content validation beyond extension checking.
- The admin role can reassign note ownership, representing a privilege escalation risk that requires verifiable audit trails.
- Password reset flows handle credential recovery and must resist token prediction, reuse, and replay.

**Why Level 3 is not required (baseline):** No regulated PII categories (health, financial), no monetary transactions, no real-time safety implications. Individual feature escalations to Level 3 expectations are noted where the risk warrants it.

**Features with elevated risk profiles** (Level 3 guidance referenced where applicable):
- F-05 File Attachment ΓÇö storage exhaustion, malicious content delivery
- F-08 Note Sharing ΓÇö unauthenticated access surface, token guessing
- F-13 Admin Dashboard ΓÇö privilege abuse, audit trail completeness

---

## 1. Feature-ASVS Coverage Matrix

| Feature | Title | ASVS Section | Req ID | Level | Coverage | PRD Change Needed |
|---------|-------|-------------|--------|-------|----------|-------------------|
| F-01 | User Registration | V6.2 Password Security | 6.2.1 | 1 | Partial | Add minimum length (15 char recommended), max length (ΓëÑ64 char) |
| F-01 | User Registration | V6.2 Password Security | 6.2.4 | 1 | Missing | Check passwords against top-3000 common passwords list |
| F-01 | User Registration | V6.2 Password Security | 6.2.5 | 1 | Missing | Allow any character composition; remove arbitrary complexity rules |
| F-01 | User Registration | V6.2 Password Security | 6.2.6 | 1 | Missing | password fields must use type=password; allow show/hide toggle |
| F-01 | User Registration | V6.2 Password Security | 6.2.7 | 1 | Missing | Permit paste, browser password helpers, external managers |
| F-01 | User Registration | V2.2 Input Validation | 2.2.1 | 1 | Partial | Server-side validation of all registration fields; document allowed formats |
| F-01 | User Registration | V7.2 Session Management | 7.2.4 | 1 | Missing | Generate new session token on registration-login; invalidate old token |
| F-02 | User Login | V7.2 Session Management | 7.2.3 | 1 | Missing | Specify CSPRNG session token with ΓëÑ128 bits entropy |
| F-02 | User Login | V7.2 Session Management | 7.2.4 | 1 | Missing | Regenerate session token on every authentication |
| F-02 | User Login | V7.3 Session Timeout | 7.3.1 | 2 | Missing | Define and document inactivity timeout |
| F-02 | User Login | V7.3 Session Timeout | 7.3.2 | 2 | Missing | Define and document absolute session lifetime |
| F-02 | User Login | V3.3 Cookie Setup | 3.3.1 | 1 | Missing | Secure attribute on auth cookie |
| F-02 | User Login | V3.3 Cookie Setup | 3.3.2 | 2 | Missing | SameSite=Strict or Lax on auth cookie |
| F-02 | User Login | V3.3 Cookie Setup | 3.3.4 | 2 | Missing | HttpOnly attribute on session cookie |
| F-02 | User Login | V2.4 Anti-automation | 2.4.1 | 2 | Missing | Rate limit login attempts per user/IP |
| F-02 | User Login | V16.2 Logging | 16.2.1 | 2 | Partial | Structured log: who, when, from where, success/failure |
| F-03 | Password Reset | V6.2 Password Security | 6.2.3 | 1 | Missing | Reset must enforce new password + token; document token invalidation |
| F-03 | Password Reset | V7.4 Session Termination | 7.4.3 | 2 | Missing | Terminate all active sessions after successful password reset |
| F-03 | Password Reset | V2.4 Anti-automation | 2.4.1 | 2 | Missing | Rate limit reset request endpoint |
| F-03 | Password Reset | V16.2 Logging | 16.2.1 | 2 | Missing | Log reset requests and completions with actor/token metadata |
| F-04 | Note Creation | V8.2 Authorization | 8.2.1 | 1 | Covered | Require authenticated user; note linked to creator ID |
| F-04 | Note Creation | V2.2 Input Validation | 2.2.1 | 1 | Partial | Server-side length limits on title and content fields |
| F-04 | Note Creation | V2.2 Input Validation | 2.2.2 | 1 | Missing | Validation must occur server-side, not only in client JS or HTML |
| F-05 | File Attachment | V5.2 File Upload | 5.2.1 | 1 | Partial | Enforce per-file size limit; document maximum |
| F-05 | File Attachment | V5.2 File Upload | 5.2.2 | 1 | Partial | Validate magic bytes/content type, not just extension |
| F-05 | File Attachment | V5.3 File Storage | 5.3.1 | 1 | Covered | Files stored with generated names; not executed on direct HTTP access |
| F-05 | File Attachment | V5.3 File Storage | 5.3.2 | 1 | Partial | Strictly validate/sanitize all file paths; never use user-supplied names |
| F-05 | File Attachment | V5.2 File Upload | 5.2.4 | 3 | Missing | Per-user storage quota; maximum number of attachments per note/user |
| F-06 | Note Editing | V8.2 Authorization | 8.2.2 | 1 | Partial | Ownership check must be server-side; document IDOR prevention |
| F-06 | Note Editing | V8.3 Authorization | 8.3.1 | 1 | Missing | Authorization enforced at service layer; not relying on client-side state |
| F-06 | Note Editing | V2.2 Input Validation | 2.2.1 | 1 | Partial | Validate all edit fields server-side on submission |
| F-07 | Note Deletion | V8.2 Authorization | 8.2.2 | 1 | Covered | Only owner or admin can delete |
| F-07 | Note Deletion | V2.3 Business Logic | 2.3.1 | 1 | Missing | Cascade deletion of attachments, ratings, share links within a transaction |
| F-07 | Note Deletion | V2.3 Business Logic | 2.3.3 | 2 | Missing | Deletion must be atomic; partial deletion states are not permitted |
| F-08 | Note Sharing | V7.2 Session Management | 7.2.3 | 1 | Missing | Share tokens must use CSPRNG with ΓëÑ128 bits entropy |
| F-08 | Note Sharing | V8.2 Authorization | 8.2.2 | 1 | Partial | Revocation must take effect immediately server-side |
| F-08 | Note Sharing | V16.2 Logging | 16.2.1 | 2 | Missing | Log share link creation and revocation events |
| F-09 | Visibility Control | V8.2 Authorization | 8.2.2 | 1 | Covered | Visibility enforced server-side on all queries |
| F-09 | Visibility Control | V8.3 Authorization | 8.3.1 | 1 | Missing | Visibility checks must not rely on client-supplied parameters |
| F-10 | Note Rating | V8.2 Authorization | 8.2.2 | 1 | Partial | Verify note is public or owned before allowing rating |
| F-10 | Note Rating | V2.2 Input Validation | 2.2.1 | 1 | Missing | Validate rating value (1ΓÇô5 integer); reject out-of-range server-side |
| F-10 | Note Rating | V2.4 Anti-automation | 2.4.1 | 2 | Missing | Rate limit rating submissions per user |
| F-11 | Rating Management | V8.2 Authorization | 8.2.2 | 1 | Covered | Only note owner views rating list |
| F-12 | Note Search | V8.2 Authorization | 8.2.2 | 1 | Covered | Private notes from others excluded from results |
| F-12 | Note Search | V2.4 Anti-automation | 2.4.1 | 2 | Missing | Rate limit search endpoint to prevent scraping/enumeration |
| F-13 | Admin Dashboard | V8.1 Authorization Docs | 8.1.1 | 1 | Partial | Document all admin-only functions and data access rules explicitly |
| F-13 | Admin Dashboard | V8.2 Authorization | 8.2.1 | 1 | Covered | Admin role required via [Authorize(Roles="Admin")] |
| F-13 | Admin Dashboard | V16.1 Logging | 16.1.1 | 2 | Missing | Inventory of all events logged in admin context |
| F-13 | Admin Dashboard | V16.2 Logging | 16.2.1 | 2 | Missing | Structured log entries for all admin actions |
| F-14 | Profile Management | V6.2 Password Security | 6.2.3 | 1 | Missing | Password change requires current password |
| F-14 | Profile Management | V7.4 Session Termination | 7.4.3 | 2 | Missing | Option to terminate other sessions after credential change |
| F-14 | Profile Management | V2.2 Input Validation | 2.2.1 | 1 | Partial | Validate email format server-side; validate username uniqueness |
| F-15 | Top Rated Notes | V8.2 Authorization | 8.2.2 | 1 | Covered | Only public notes with ΓëÑ3 ratings shown |
| F-15 | Top Rated Notes | V2.4 Anti-automation | 2.4.1 | 2 | Missing | Rate limit top-rated endpoint to prevent enumeration |
| F-16 | Ownership Reassignment | V8.2 Authorization | 8.2.2 | 1 | Covered | Admin-only action |
| F-16 | Ownership Reassignment | V16.2 Logging | 16.2.1 | 2 | Partial | Log reassignment: admin actor, note ID, old owner, new owner, timestamp |
| F-16 | Ownership Reassignment | V2.3 Business Logic | 2.3.3 | 2 | Missing | Reassignment must be atomic; related data (share links, ratings) must remain consistent |

---

## 2. Enhanced Feature Specifications

### Feature F-01: User Registration

**Original REQ-001**: Users register with username, email, and password. Creates a user account.

**Additional Requirements Added:**
- Passwords must be a minimum of 8 characters (15 strongly recommended); maximum must allow at least 64 characters.
- Registration password must be checked against a list of at least the top 3,000 common passwords matching the application's length policy.
- No character-type composition rules (uppercase, numbers, special characters) may be imposed.
- Password input must use `type=password`; a show/hide toggle is permitted.
- Paste, browser password managers, and external password managers must not be blocked.
- Username must be validated for length (1ΓÇô50 characters) and allowed characters server-side.
- Email must be validated for format (RFC 5321 structure) server-side; uniqueness verified before account creation.
- On successful registration with automatic login, a new session token must be generated via a CSPRNG.

**ASVS Mapping**: V6.2.1, V6.2.4, V6.2.5, V6.2.6, V6.2.7, V6.2.8, V7.2.3, V7.2.4, V2.2.1, V2.2.2

**SSEM Implementation Notes**:
- **Analyzability**: Registration controller method performs a single, well-named flow: validate inputs ΓåÆ check uniqueness ΓåÆ hash password ΓåÆ create account ΓåÆ generate session ΓåÆ log event. No embedded security logic in views.
- **Modifiability**: Password policy (minimum length, banned list source) configured via options class; changing policy requires no controller edits.
- **Testability**: Each validation step (password length, banned list check, email format, username uniqueness) must be independently testable via unit tests without database dependency.
- **Confidentiality**: Password transmitted over TLS only; never stored in plaintext; never echoed in response bodies or logs; email stored and treated as low-sensitivity PII.
- **Accountability**: Registration event logged with timestamp, username, email (hashed or truncated), and IP address. Duplicate registration attempts (same email) logged as informational events.
- **Authenticity**: Identity confirmed at time of registration via email validation (format only at L2; email verification link at L3). No assumed identity before verification.
- **Availability**: Registration endpoint rate-limited per IP. Validation errors return structured error messages without revealing whether email or username already exists (use generic "registration failed" with enumeration-safe responses).
- **Integrity**: Password stored using ASP.NET Core Identity PBKDF2 hash. No truncation or normalization applied to password before hashing.
- **Resilience**: If the database insert fails, the session must not be established. Registration must succeed or fail atomically; no partial user records are permitted.

**FIASSE Tenet Annotations**:
- **S2.1**: Registration logic must not assume the current password policy, banned list, or hashing algorithm is permanent. These are externalized and replaceable without code changes, acknowledging that "secure" configurations evolve.
- **S2.2**: Registration delivers the business value of onboarding users; robustness under stress (e.g., high registration volume, dictionary attacks) is preserved through rate limiting and efficient validation ordering (cheap checks first).
- **S2.3**: Enumeration-safe responses reduce the probability of user account mapping by automated actors, directly reducing the material impact surface of credential-stuffing campaigns.
- **S2.4**: Security controls (rate limiting, banned password list, CSPRNG session generation) are engineering-level, system-wide controls ΓÇö not per-developer line fixes. No developer needs to "think like an attacker" to implement these; they follow defined patterns.
- **S2.6**: Every registration attempt (success or blocked) produces a structured, timestamped log entry containing actor-traceable metadata, enabling post-incident investigation without exposing credentials in logs.

**Acceptance Criteria:**
- [ ] Password shorter than 8 characters is rejected with a validation message before database insert.
- [ ] Password matching any entry in the top-3000 list is rejected with a message indicating the password is too common.
- [ ] Password of exactly 64 characters is accepted without truncation.
- [ ] Paste into password field is not prevented by the application.
- [ ] Registering with an already-used email returns a response indistinguishable from a format-validation error (enumeration-safe).
- [ ] Registration with a duplicate username returns an enumeration-safe error.
- [ ] Registration event appears in the application log with timestamp, masked email, and source IP.
- [ ] On registration with auto-login, the session cookie has `Secure`, `HttpOnly`, and `SameSite=Strict` attributes.

---

### Feature F-02: User Login / Authentication

**Original REQ-002**: Username/password login; creates cookie session; failed attempts return error; session persists until logout or timeout.

**Additional Requirements Added:**
- Session token must be generated via CSPRNG with at least 128 bits of entropy.
- Session token must be regenerated on every successful authentication (prevents session fixation).
- Authentication cookie must have `Secure`, `HttpOnly`, and `SameSite=Strict` attributes.
- Inactivity timeout must be defined and documented (recommended: 30 minutes).
- Absolute maximum session lifetime must be defined and documented (recommended: 8 hours).
- Login endpoint must be rate-limited per username and per IP (recommended: Γëñ10 attempts per minute per IP before temporary lockout).
- Failed login responses must not distinguish between unknown username and incorrect password (enumeration-safe).
- Login events (success and failure) must be logged with structured metadata.

**ASVS Mapping**: V7.2.3, V7.2.4, V7.3.1, V7.3.2, V7.4.1, V3.3.1, V3.3.2, V3.3.4, V2.4.1, V16.2.1, V16.2.5

**SSEM Implementation Notes**:
- **Analyzability**: Authentication logic is centralized in a single service or middleware layer; not duplicated across controller actions.
- **Modifiability**: Session timeout values and rate-limit thresholds are externalized in configuration (`appsettings.json`); changing them requires no code deployment.
- **Testability**: Session fixation prevention (new token issued on login) and cookie attribute enforcement must be verifiable via integration tests that inspect Set-Cookie headers.
- **Confidentiality**: Password never stored in session or logs; session token transmitted only via `HttpOnly` cookie; IP address logged but treated as operational data with appropriate retention limits.
- **Accountability**: Each login attempt logged: timestamp (UTC), username attempted, source IP, outcome (success/failure), user-agent. Failed attempts contribute to rate-limit counters.
- **Authenticity**: Cookie-based session references a server-side session store; session validity confirmed on every request at the trusted backend layer.
- **Availability**: Rate limiting applied at the application layer with configurable thresholds. Lockout state must not be permanent to avoid denial-of-service against legitimate users; exponential backoff or timed lockout preferred.
- **Integrity**: Session token binding is server-authoritative; client cannot supply or modify session identifiers in ways the server will honor.
- **Resilience**: Failed authentication must not leave partial session state. If session store is unavailable, authentication must fail closed (deny), not fail open.

**FIASSE Tenet Annotations**:
- **S2.1**: Cookie attributes and timeout values are configuration-driven, allowing adaptation to policy changes without code modifications as the threat landscape evolves.
- **S2.2**: Rate limiting protects the authentication surface from abuse while keeping the service available to legitimate users ΓÇö preserving business value under adversarial stress.
- **S2.3**: Enumeration-safe error responses and rate limiting directly reduce the probability that credential-stuffing attacks yield material account compromises.
- **S2.4**: Session fixation prevention is an engineering control (always regenerate token on authentication) that eliminates a class of vulnerability by design, not by vigilance.
- **S2.6**: Structured login event logs with UTC timestamps, IP, username, and outcome enable security teams to detect anomalous patterns (brute force, account takeover) from log data alone.

**Acceptance Criteria:**
- [ ] On successful login, the `Set-Cookie` header includes `Secure; HttpOnly; SameSite=Strict`.
- [ ] On successful login, the session token differs from any pre-login session token (fixation prevention verified).
- [ ] Ten failed login attempts from the same IP within 60 seconds results in a temporary block; the block is documented in configuration.
- [ ] Error message for wrong password and unknown username is identical.
- [ ] Session is invalidated server-side after the configured inactivity timeout.
- [ ] Session is invalidated server-side after the configured absolute lifetime.
- [ ] Each login attempt produces a log entry with timestamp (UTC), username, IP, and outcome.

---

### Feature F-03: Password Reset

**Original REQ-003**: User requests reset by email; receives time-limited token link (1-hour); token allows setting new password; used/expired tokens rejected.

**Additional Requirements Added:**
- Reset tokens must be generated via CSPRNG with at least 128 bits of entropy (not sequential IDs or timestamps).
- Token expiry must be enforced server-side; client-supplied timestamps are not trusted.
- Used tokens must be immediately invalidated after first use; they must not be reusable.
- All active sessions must be terminated on successful password reset.
- New password submitted during reset must pass the same policy as registration (length, common-password list).
- Reset request endpoint must be rate-limited per email address and per IP.
- Reset request response must not confirm whether the email exists (enumeration-safe: always return "if email is registered, a link was sent").
- All reset events (request, completion, failure) must be logged with structured metadata.

**ASVS Mapping**: V6.2.1, V6.2.3, V6.2.4, V7.2.3, V7.4.3, V2.4.1, V16.2.1, V16.2.5

**SSEM Implementation Notes**:
- **Analyzability**: Token lifecycle (generation, storage, validation, revocation) is implemented in a dedicated, bounded service class with explicit state transitions; not embedded in the controller.
- **Modifiability**: Token TTL (currently 1 hour) is a named configuration value; extending or shortening it requires no code changes.
- **Testability**: Token invalidation after use and expiry enforcement must be independently testable via integration tests that reuse tokens and present expired tokens.
- **Confidentiality**: Token is transmitted only via email link and never logged in plaintext; email address is confirmed to exist only to the system, not to callers.
- **Accountability**: Reset requests logged with timestamp, hashed email address, and source IP. Reset completions logged with timestamp and username. Token reuse attempts logged as security events.
- **Authenticity**: Token binding ties the reset to a specific user account; possession of the token is the only authentication factor. Token must not be predictable or derivable.
- **Availability**: Rate limiting protects the email delivery system from being abused for spam. Reset flow must degrade gracefully if the email service is unavailable (queue, retry, user-visible message).
- **Integrity**: New password is validated server-side before the old credential hash is replaced. If new password fails validation, the reset token remains valid for the remainder of its TTL.
- **Resilience**: If the database update fails during reset, the old credential remains valid and the token is not consumed. The operation is atomic.

**FIASSE Tenet Annotations**:
- **S2.1**: Token strength and TTL are not assumed to be permanently adequate; they are configurable and can be tightened as credential-attack techniques improve.
- **S2.2**: The enumeration-safe response preserves the reset flow's value (account recovery) without revealing account existence, balancing usability and security.
- **S2.3**: Immediate session termination on reset prevents scenarios where an attacker who triggered the reset retains an active session, limiting material impact of credential compromise.
- **S2.4**: Single-use token enforcement and CSPRNG-based generation are systematic engineering controls that eliminate token-reuse and prediction attack classes by construction.
- **S2.6**: Structured logging of all reset lifecycle events (request, completion, failure, reuse attempt) creates the audit trail needed for incident response without storing tokens or passwords in logs.

**Acceptance Criteria:**
- [ ] Reset token submitted twice returns an error on the second use.
- [ ] Reset token submitted after 1 hour returns an expiry error.
- [ ] Reset request for an unregistered email returns a response identical to one for a registered email.
- [ ] On successful reset, all previously active sessions for the user are invalidated server-side.
- [ ] New password submitted during reset that is in the top-3000 list is rejected.
- [ ] Reset request endpoint rejects more than 5 requests per email address per 15-minute window.
- [ ] Reset completion event is logged with username, timestamp, and source IP (no token or password in log).

---

### Feature F-04: Note Creation

**Original REQ-004**: Authenticated users create notes with title (required) and content (required). Linked to creator. Default visibility: private. Creation timestamp auto-recorded.

**Additional Requirements Added:**
- Title field: server-side maximum length enforced (recommended: 500 characters). Empty or whitespace-only title rejected.
- Content field: server-side maximum length enforced (recommended: 100,000 characters) to prevent storage exhaustion.
- Visibility defaults to private; the server must set this default, not rely on a client-supplied value.
- All note creation requests must include an anti-forgery token (CSRF protection).
- The note's `UserId` is taken from the authenticated session, not from a request body field (prevents mass-assignment of ownership).

**ASVS Mapping**: V8.2.1, V8.2.2, V8.3.1, V2.2.1, V2.2.2, V2.1.1, V4.1.1

**SSEM Implementation Notes**:
- **Analyzability**: Note creation controller method extracts `UserId` from `User.GetUserId()` (not from form data); this is explicit and reviewable.
- **Modifiability**: Field length limits are constants in a configuration or model annotation; changing them does not require changes to controller logic.
- **Testability**: CSRF token enforcement, ownership assignment, and server-side default visibility are verifiable via integration tests.
- **Confidentiality**: Note content is private by default; no content is included in API responses beyond what the requesting user is authorized to see.
- **Accountability**: Note creation logged with user ID, note ID, timestamp, and visibility setting.
- **Authenticity**: Creator identity is taken from the server-issued authentication principal; it cannot be spoofed via request body parameters.
- **Availability**: Server-side content length limits prevent large payloads from consuming disproportionate storage or processing resources.
- **Integrity**: Anti-forgery token ensures the creation request originates from the application's own form, not from a cross-site request.
- **Resilience**: If the database insert fails, no note ID is returned and no partial record exists. The user receives a meaningful error.

**FIASSE Tenet Annotations**:
- **S2.1**: Visibility defaults and ownership assignment are server-enforced; they are not dependent on client state, making the behavior consistent regardless of what the client sends.
- **S2.2**: Field length limits keep the system functional under adversarial input (oversized payloads) without degrading service for other users.
- **S2.3**: Preventing mass-assignment of ownership eliminates a class of privilege confusion attacks before they can affect content integrity.
- **S2.4**: Extracting `UserId` from the session principal (not request body) is a one-time architectural rule that eliminates an entire category of authorization bypass attempts systematically.
- **S2.6**: Note creation events are logged with sufficient metadata (user ID, note ID, visibility) to reconstruct the creation history if a content dispute arises.

**Acceptance Criteria:**
- [ ] A note creation request without an anti-forgery token returns HTTP 400.
- [ ] `UserId` in the created note matches the authenticated session user; a different `UserId` in the form body is ignored.
- [ ] Newly created notes have `IsPublic = false` regardless of whether the client sends a visibility parameter.
- [ ] A title exceeding the maximum length is rejected with a validation error before database insert.
- [ ] An empty or whitespace-only title is rejected with a validation error.

---

### Feature F-05: File Attachment

**Original REQ-005**: Users upload files (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG). Stored with unique IDs. Original filename preserved in metadata.

**Additional Requirements Added:**
- Maximum file size must be defined, documented, and enforced server-side before content is fully buffered (recommended: 10 MB per file).
- File type validation must verify content (magic bytes / file signature), not only the extension. Extension-only validation is insufficient.
- Files must be stored under server-generated unique names (UUID/GUID); original filename is stored in metadata only and is never used as a file system path component.
- Files must be stored outside the web root or with server configuration preventing direct HTTP execution.
- Per-user storage quota must be enforced to prevent storage exhaustion (maximum total bytes and maximum number of attachments per user).
- Files served for download must include `Content-Disposition: attachment` to prevent browser execution.
- Image uploads must be validated against maximum pixel dimensions to prevent pixel flood attacks.
- The file storage path is configured by the application; user input must never influence directory traversal.

**ASVS Mapping**: V5.2.1, V5.2.2, V5.2.4, V5.2.6, V5.3.1, V5.3.2, V3.2.1

**SSEM Implementation Notes**:
- **Analyzability**: File handling is encapsulated in a dedicated `IFileStorageService`; upload validation, storage, and download are not implemented inline in the controller. Trust boundary is explicit at the service interface.
- **Modifiability**: Allowed file types, size limits, and quota values are configuration-driven. Adding a new allowed type requires only a configuration change, not a code change.
- **Testability**: Each validation step (size limit, magic byte check, quota check, path construction) must have discrete unit tests. File download header enforcement is verifiable via integration tests.
- **Confidentiality**: Original filenames stored in metadata are treated as user data; they are displayed only to authorized users (note owner, admin) and never used to construct file system paths.
- **Accountability**: All file uploads (success and failure), downloads, and deletions are logged with user ID, note ID, file ID, and outcome.
- **Authenticity**: File ownership is tied to the authenticated user at upload time; this binding is stored server-side and cannot be altered by the client.
- **Availability**: File size and count quotas protect shared storage infrastructure. Upload endpoint rate-limited per user to prevent quota-exhaustion attacks.
- **Integrity**: Magic-byte validation ensures files match their declared type, reducing the risk of polyglot-file attacks. Content-Disposition enforces download-not-execute behavior in the browser.
- **Resilience**: If storage fails mid-upload, the database attachment record must not be created. Cleanup of orphaned partial files is handled by the storage service, not the controller.

**FIASSE Tenet Annotations**:
- **S2.1**: File type allowlist and validation approach are configuration-driven and replaceable; adding or removing types does not require redesign, acknowledging that safe file types evolve.
- **S2.2**: Quota enforcement and size limits ensure the file upload feature continues to deliver value (note enrichment) without enabling storage abuse that degrades the service for all users.
- **S2.3**: Content-type validation (magic bytes) and `Content-Disposition: attachment` together reduce the probability that a malicious file upload leads to a material code-execution or MIME-confusion incident.
- **S2.4**: Server-generated file paths (never derived from user input) eliminate path traversal by construction ΓÇö a systematic engineering control that does not depend on developers sanitizing each individual upload.
- **S2.6**: Structured upload/download logs with file ID, user ID, and outcome enable forensic reconstruction of file-based incident timelines without exposing file content in logs.

**Acceptance Criteria:**
- [ ] Uploading a file exceeding the size limit returns HTTP 413 before the full file body is read.
- [ ] Uploading a `.jpg` file whose magic bytes identify it as a PDF is rejected.
- [ ] Uploading a `.exe` file is rejected regardless of magic bytes.
- [ ] The stored file on disk uses a UUID/GUID name; the original filename does not appear in any file system path.
- [ ] A direct HTTP request to the stored file path returns a response with `Content-Disposition: attachment`.
- [ ] A user who has reached the per-user file quota receives a rejection error on the next upload attempt.
- [ ] Upload event appears in the log with file ID, user ID, note ID, file size, and outcome.

---

### Feature F-06: Note Editing

**Original REQ-006**: Note owners can modify title, content, and visibility. Last-modified timestamp updated on save.

**Additional Requirements Added:**
- Ownership must be verified server-side on every edit request by matching the note's `UserId` against the authenticated session user (prevents IDOR).
- Ownership check must not be performed by trusting a `userId` field in the request body.
- All edit fields (title, content, visibility) must be validated server-side with the same constraints as creation.
- All edit requests must include an anti-forgery token.
- Edit events (ownership, field changes) must be logged.

**ASVS Mapping**: V8.2.2, V8.3.1, V2.2.1, V2.2.2, V16.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Edit authorization check is a single, centralized method called before any field mutation. The pattern is identical to the ownership check on deletion.
- **Modifiability**: Shared ownership-check logic means that fixing an authorization bug in one place fixes it for both edit and delete operations.
- **Testability**: IDOR protection (attempting to edit another user's note ID returns 403) is a mandatory test case. Unauthorized edit attempts must be confirmed to return HTTP 403 with no data modification.
- **Confidentiality**: The edit endpoint does not reveal whether a given note ID exists to unauthorized requestors (return 403 or 404 consistently).
- **Accountability**: Edit events logged with user ID, note ID, fields changed (not their values), and timestamp.
- **Authenticity**: Authenticated session principal is the sole source of user identity for the ownership check.
- **Availability**: Field length limits apply on edit as on creation, preventing storage exhaustion via large content updates.
- **Integrity**: Last-modified timestamp is set server-side at save time; it is not accepted from the client.
- **Resilience**: If the database update fails, the previous version of the note remains intact. The edit is atomic.

**FIASSE Tenet Annotations**:
- **S2.1**: Server-side ownership checks are invariant regardless of how the client presents the request, reflecting that authorization must not be a client-side assumption.
- **S2.2**: Consistent validation rules across creation and editing prevent the editing path from becoming a weaker entry point.
- **S2.3**: Centralized ownership checks eliminate the IDOR vulnerability class systematically, reducing the attack surface for unauthorized note modification.
- **S2.4**: Shared authorization logic for edit/delete reduces the surface for per-developer mistakes; the authorization engineering is done once and reused.
- **S2.6**: Edit logs capturing which fields changed (not values) enable audit of content modification patterns without storing note content in logs.

**Acceptance Criteria:**
- [ ] A request to edit a note not owned by the authenticated user returns HTTP 403; the note is unchanged.
- [ ] A request that includes a different `UserId` in the body does not change the note's owner.
- [ ] Last-modified timestamp is set by the server at save time; client-supplied timestamps are ignored.
- [ ] Anti-forgery token absent from edit form submission returns HTTP 400.

---

### Feature F-07: Note Deletion

**Original REQ-007**: Note owners or admins delete notes. Confirmation prompt before deletion. Deletion removes note, attachments, ratings, and share links permanently.

**Additional Requirements Added:**
- Cascade deletion of attachments (from file storage and database), ratings, and share links must be atomic. If any cascade step fails, the deletion must be rolled back entirely.
- Authorization must be verified server-side; the UI confirmation prompt is a UX control only, not a security control.
- Admin deletion of another user's note must be logged as an administrative action distinct from owner deletion.
- Physical file deletion from storage must be coordinated with the database transaction; files must not be orphaned.

**ASVS Mapping**: V8.2.2, V8.3.1, V2.3.1, V2.3.3, V16.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Deletion workflow is documented as: verify ownership/admin role ΓåÆ begin transaction ΓåÆ delete files from storage ΓåÆ delete database records ΓåÆ commit ΓåÆ log. Each step is explicit.
- **Modifiability**: If the cascade set changes (e.g., a new entity type references notes), the deletion service is the single modification point.
- **Testability**: Deletion atomicity must be tested: simulate storage deletion success followed by database failure and verify the storage files are restored or the operation is rolled back.
- **Confidentiality**: Deleted note content is not returned in any response after deletion; share links referencing deleted notes return 404 or 410.
- **Accountability**: Deletion events logged with actor user ID, note ID, note owner ID, timestamp, and whether the actor was the owner or an admin.
- **Authenticity**: Authorization check precedes any deletion; unauthenticated or unauthorized deletion attempts return 401/403 with no state change.
- **Availability**: Bulk deletion (if permitted) is rate-limited to prevent accidental or malicious mass deletion.
- **Integrity**: Referential integrity is maintained by ensuring cascading deletion completes within a single database transaction.
- **Resilience**: If file storage deletion fails, the database record is not deleted; a retry or manual cleanup process is triggered rather than leaving the system in an inconsistent state.

**FIASSE Tenet Annotations**:
- **S2.1**: Atomic cascade deletion acknowledges that consistency requirements may evolve (new related entities) and ensures the implementation can be extended without rewriting the transaction logic.
- **S2.2**: Atomic deletion preserves system integrity so that failed deletions do not leave orphaned files consuming storage or orphaned share links exposing deleted content.
- **S2.3**: Logging admin deletions separately from owner deletions creates an audit trail that enables detection of unauthorized content removal by privileged accounts.
- **S2.4**: Transaction-wrapped cascade deletion is an engineering control that prevents partial-delete states by construction, rather than relying on each developer to remember all cascade steps.
- **S2.6**: Deletion logs include the actor, the target, and whether the deletion was admin-initiated, providing the minimum context needed for an investigation without storing note content.

**Acceptance Criteria:**
- [ ] A request to delete a note not owned by the authenticated user (non-admin) returns HTTP 403; note is unchanged.
- [ ] When note deletion succeeds, all associated attachments are removed from file storage and the database.
- [ ] When note deletion succeeds, all associated ratings and share links are removed from the database.
- [ ] If file storage deletion fails during a note deletion, the note record and related database rows remain intact (rollback verified).
- [ ] Admin deletion of another user's note produces a log entry distinct from owner deletion (actor role is recorded).
- [ ] A share link for a deleted note returns HTTP 404 or 410.

---

### Feature F-08: Note Sharing

**Original REQ-008**: Note owners generate unique share links. Anyone with the link can view note without authentication. Links are revocable and regenerable.

**Additional Requirements Added:**
- Share tokens must be generated via CSPRNG with at least 128 bits of entropy; they must not be sequential, timestamp-derived, or predictable.
- Revocation must take effect immediately server-side; a revoked token must return 404/410 on the next request, regardless of client caching.
- If a note is set to private after a share link is generated, the share link must remain valid (link is the explicit sharing mechanism); but if the link itself is revoked, access must be denied regardless of note visibility.
- Share link creation and revocation events must be logged with user ID, note ID, link ID, and timestamp.
- The share link view endpoint must not expose any user data beyond the note content and attachments (no email, no user ID).

**ASVS Mapping**: V7.2.3, V8.2.2, V8.3.1, V16.2.1, V3.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Share link lifecycle is managed by a dedicated `IShareLinkService`; token generation, validation, and revocation are not embedded in controllers.
- **Modifiability**: Token length and entropy source are configuration-driven; upgrading token entropy does not require changing the sharing UX.
- **Testability**: Token unpredictability is validated indirectly by verifying CSPRNG usage in the service. Revocation effectiveness is verifiable via integration tests that access a revoked token.
- **Confidentiality**: Share link view responses strip all user PII (email, internal user ID) from the response; only note content and metadata visible to the link holder.
- **Accountability**: Share link creation and revocation logged with link ID, note ID, actor user ID, timestamp. Access via share link is logged with link ID and source IP.
- **Authenticity**: Token authenticity is validated by database lookup of the token value; the token is the credential for unauthenticated access.
- **Availability**: Share link view endpoint is rate-limited per IP to prevent token enumeration or content scraping.
- **Integrity**: Share links are bound to note IDs at creation time; they cannot be redirected to another note.
- **Resilience**: A revoked share link must deny access even if the link URL is cached or bookmarked by the recipient; server-side state is authoritative.

**FIASSE Tenet Annotations**:
- **S2.1**: CSPRNG-based token generation is not assumed to be the final word on token strength; the entropy requirement is a minimum that can be raised as threat models evolve.
- **S2.2**: Share link functionality enables controlled external sharing without requiring recipient authentication ΓÇö delivering the sharing business value while maintaining access control.
- **S2.3**: Server-side revocation (not client-side) ensures that token compromise can be responded to immediately, limiting the window of unauthorized access.
- **S2.4**: Centralizing token generation in a service with a CSPRNG dependency eliminates the risk that individual developers implement weaker token generation; the correct implementation is the only one available.
- **S2.6**: Share link access logs with link ID and source IP create the minimal audit trail needed to detect token leakage or abuse patterns.

**Acceptance Criteria:**
- [ ] A generated share link token is at least 22 characters long (128-bit CSPRNG base64url minimum).
- [ ] Two consecutive share link tokens for the same note are different.
- [ ] A revoked share link returns HTTP 404 or 410 within 1 second of revocation; no caching delay is permitted.
- [ ] The share link view page renders note content without any user email, internal user ID, or session data.
- [ ] Share link creation and revocation events appear in logs with actor user ID, note ID, link ID, and timestamp.

---

### Feature F-09: Public / Private Note Visibility

**Original REQ-009**: Notes have public/private toggle. Public notes are searchable and viewable by anyone. Private notes visible only to owner. Default is private.

**Additional Requirements Added:**
- Visibility checks must be enforced at the data access layer (query filter), not only in controller logic, to prevent bypass via direct data access.
- The server must not accept visibility as a URL query parameter that can override the stored value.
- Visibility changes must be logged (who, which note, changed from/to, when).

**ASVS Mapping**: V8.2.2, V8.2.3, V8.3.1, V16.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Visibility enforcement is a repository-level query filter (e.g., `GlobalQueryFilter` or explicit `.Where(n => n.IsPublic || n.UserId == userId)`); visibility is never filtered only in controller logic.
- **Modifiability**: A future "shared-with-user" visibility tier can be added by extending the query filter; it does not require changes across all controllers.
- **Testability**: Each visibility state (public note accessed by non-owner, private note accessed by non-owner, private note accessed by owner) must have a dedicated integration test.
- **Confidentiality**: Private note content is never included in any response to a non-owner, including error responses that might reveal content length or existence.
- **Accountability**: Visibility changes logged with user ID, note ID, previous value, new value, and timestamp.
- **Authenticity**: The authenticated session identity is used to determine ownership; no client-supplied "isOwner" flag is honored.
- **Availability**: Visibility filter is applied at the database query level to avoid fetching and then filtering large datasets in memory.
- **Integrity**: Default visibility (`IsPublic = false`) is set server-side at creation; client-supplied defaults are ignored.
- **Resilience**: If the visibility state is indeterminate (corrupted record), the system defaults to treating the note as private.

**FIASSE Tenet Annotations**:
- **S2.1**: Query-level enforcement of visibility means the authorization check cannot be bypassed by adding a new API endpoint that forgets to include the controller-level check.
- **S2.2**: Efficient query-level filtering (indexed `IsPublic` and `UserId` columns) ensures visibility enforcement does not degrade performance as the note dataset grows.
- **S2.3**: Fail-safe default to private for indeterminate states ensures that data exposure is never the consequence of a data integrity issue.
- **S2.4**: Repository-level query filters are a systemic enforcement mechanism; no individual developer can accidentally expose private notes by omitting a controller check.
- **S2.6**: Visibility change events in logs allow auditors to reconstruct when a note's exposure changed and who authorized the change.

**Acceptance Criteria:**
- [ ] An authenticated non-owner accessing a private note's direct URL returns HTTP 403 or 404.
- [ ] A private note does not appear in the search results of a non-owner user.
- [ ] A public note appears in the search results of all users including unauthenticated visitors (if applicable).
- [ ] Setting a note to public from private is reflected in the log with the actor user ID, note ID, and timestamp.
- [ ] A note with a corrupted or null visibility field is treated as private.

---

### Feature F-10: Note Rating

**Original REQ-010**: Authenticated users rate notes 1ΓÇô5 stars with optional comment. Ratings visible to all. Users can edit their own rating.

**Additional Requirements Added:**
- Rating value must be validated server-side as an integer in the range 1ΓÇô5; values outside this range are rejected before database insert.
- A user may rate a note only if the note is public or they are the owner; rating access control must be server-side.
- Rating endpoint must be rate-limited per authenticated user to prevent rating-stuffing.
- Rating creation and edit events must be logged.
- Users must not be able to submit a rating for a note that does not exist (note ID validated server-side).

**ASVS Mapping**: V8.2.2, V8.3.1, V2.2.1, V2.3.1, V2.4.1, V16.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Rating validation (range check, note existence, note visibility) is explicit in the rating service, not buried in controller action logic.
- **Modifiability**: Rating range (1ΓÇô5) and allowed note states for rating are configurable; expanding to a 10-point scale requires a configuration change plus a database migration.
- **Testability**: Out-of-range rating values (0, 6, negative, non-integer) must each have a dedicated server-side rejection test.
- **Confidentiality**: Rating comments are user-generated content; they are displayed to authorized viewers only (see note visibility rules).
- **Accountability**: Rating creation and edits logged with rater user ID, note ID, rating value, and timestamp.
- **Authenticity**: Rater identity is taken from the authenticated session; a `userId` field in the rating body is ignored.
- **Availability**: Rate limiting prevents a single user from submitting large volumes of ratings in a short period.
- **Integrity**: Rating value is validated before storage; average recalculation is performed server-side.
- **Resilience**: If the average rating recalculation fails, the individual rating is still stored; the average is eventually consistent.

**FIASSE Tenet Annotations**:
- **S2.1**: Server-side range validation is invariant to what the client sends, acknowledging that client-side validation can always be bypassed.
- **S2.2**: Rate limiting preserves the integrity of the rating system as a useful feature (meaningful aggregates) by preventing ballot-stuffing from distorting results.
- **S2.3**: Note existence and visibility checks before rating prevent information leakage about private note existence through rating acceptance/rejection responses.
- **S2.4**: Server-side range check is an always-on engineering control; individual developers do not need to remember to add it to each rating endpoint.
- **S2.6**: Rating events in logs support audit of rating manipulation investigations without storing comment content in the security log.

**Acceptance Criteria:**
- [ ] A rating value of 0 or 6 submitted to the server returns a validation error; no database record is created.
- [ ] A rating for a private note owned by another user returns HTTP 403.
- [ ] A rating for a non-existent note ID returns HTTP 404.
- [ ] More than 10 rating submissions from the same user within 60 seconds triggers a rate-limit response.
- [ ] Rating creation event appears in the log with rater user ID, note ID, rating value, and timestamp.

---

### Feature F-11: Rating Management

**Original REQ-011**: Note owners view all ratings on their notes: value, comment, rater username, timestamp. Average rating displayed. Sorted newest first.

**Additional Requirements Added:**
- The rating list endpoint must verify that the requesting user is the note owner (server-side); non-owners must not access individual rating entries.
- Rater usernames are included; rater email addresses must not be included in rating list responses.
- Average rating is computed server-side; a client-supplied average is never trusted.

**ASVS Mapping**: V8.2.2, V8.2.3, V8.3.1

**SSEM Implementation Notes**:
- **Analyzability**: Rating list endpoint clearly performs two checks: authenticate ΓåÆ authorize (owner) ΓåÆ fetch ratings. These are not merged into a single ambiguous query.
- **Modifiability**: Fields returned in the rating list response are defined by a view model; adding or removing fields (e.g., removing rater username for privacy) requires only view model changes.
- **Testability**: Non-owner access attempt to the rating list endpoint must return 403 in a dedicated integration test.
- **Confidentiality**: Rater email is excluded from all rating list responses to avoid exposing PII of rating submitters.
- **Accountability**: Accessing the rating list is not a security event requiring logging, but changes to ratings (edits or deletions) are.
- **Authenticity**: Note ownership is verified via authenticated session; the note ID in the route is validated against the authenticated user's note collection.
- **Availability**: Rating list queries are paginated if a note has large numbers of ratings, preventing response size exhaustion.
- **Integrity**: Average rating displayed is computed by the server on each request or maintained as a materialized aggregate updated on each rating change.
- **Resilience**: If the average calculation fails, the list is still returned; the average field is omitted or marked as unavailable.

**FIASSE Tenet Annotations**:
- **S2.1**: Excluding rater email from responses reflects a privacy-first default that can be revisited if a legitimate use case arises, rather than hardcoding exposure.
- **S2.2**: Pagination ensures the rating management feature remains performant for popular notes with many ratings.
- **S2.3**: Field-level access control (no rater email) prevents PII exposure that could enable social engineering against raters.
- **S2.4**: Server-side ownership check is the only authorization mechanism; no client-supplied "isOwner" flag or session attribute is trusted.
- **S2.6**: No security logging required for read-only list access; logging would be generated by the edit/delete operations covered in F-10.

**Acceptance Criteria:**
- [ ] A non-owner authenticated user requesting the rating list for a note returns HTTP 403.
- [ ] Rating list response does not include rater email addresses.
- [ ] Average rating in the response is computed by the server; a manipulated client-supplied average is ignored.
- [ ] Rating list is returned sorted by creation timestamp descending.

---

### Feature F-12: Note Search

**Original REQ-012**: Users search notes by keywords matching title or content. Results include owned notes (any visibility) and public notes from others. Private notes from others excluded.

**Additional Requirements Added:**
- Search endpoint must be rate-limited per authenticated user and per IP to prevent scraping.
- Search query input must be validated server-side: maximum length enforced (recommended: 200 characters); empty or whitespace-only queries return no results without executing a database query.
- Search results must never include private notes from users other than the authenticated user; this filter must be applied at the database query layer.
- Result excerpts (first 200 characters) must be output-encoded to prevent XSS if any note content contains HTML or script.
- Search does not support unauthenticated access; the endpoint requires authentication.

**ASVS Mapping**: V8.2.2, V8.3.1, V2.2.1, V2.4.1, V16.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Search query filter combining visibility and ownership is a single, well-named repository method, not inline LINQ scattered across controller actions.
- **Modifiability**: Adding a new visibility tier (e.g., "shared with specific users") requires modifying only the search repository method.
- **Testability**: Search results for (a) own private note, (b) other user's public note, and (c) other user's private note must each be covered by integration tests verifying inclusion or exclusion.
- **Confidentiality**: Search result excerpts contain the first 200 characters of note content; the full content of another user's note is not returned, even for public notes.
- **Accountability**: High-volume search patterns (rate-limit breaches) are logged with user ID, timestamp, and query term count.
- **Authenticity**: The authenticated session identity determines which notes the user can see; no client-supplied user filter is accepted.
- **Availability**: Query length limits and index usage (on title and content fields, or a full-text search index) prevent expensive full-table scans on large datasets.
- **Integrity**: Output encoding of excerpts prevents note content from being rendered as HTML in search results.
- **Resilience**: If the search index is unavailable, the endpoint returns a service-unavailable message rather than falling back to an unindexed full-table scan that could time out.

**FIASSE Tenet Annotations**:
- **S2.1**: Database-layer visibility filters ensure correct access control regardless of how the search feature is extended or modified in the future.
- **S2.2**: Query length limits and index optimization ensure the search feature remains responsive as data volume grows.
- **S2.3**: Rate limiting on search prevents content enumeration attacks where an adversary iterates keywords to map all public note content.
- **S2.4**: Output encoding of excerpts is a rendering-layer engineering control that prevents XSS regardless of what content note authors submit.
- **S2.6**: Rate-limit breach events for the search endpoint provide early warning of automated scraping activity in security logs.

**Acceptance Criteria:**
- [ ] An unauthenticated request to the search endpoint returns HTTP 401.
- [ ] A search by an authenticated user does not return private notes belonging to other users.
- [ ] A search query exceeding 200 characters returns a validation error without executing a database search.
- [ ] Search result excerpts containing HTML tags (e.g., `<script>`) are rendered as escaped text in the view.
- [ ] More than 30 search requests per user per minute triggers a rate-limit response.

---

### Feature F-13: Admin Dashboard

**Original REQ-013**: Admins view user count, note count, activity log. View all users with registration date and note count. Search users by username or email.

**Additional Requirements Added:**
- The admin dashboard and all sub-pages must require the `Admin` role enforced server-side; `[Authorize(Roles="Admin")]` on controller, not only on individual actions.
- Authorization documentation must explicitly list all admin-accessible functions, data fields returned, and justification for each.
- All admin actions (viewing user lists, searching users, accessing the activity log) must be logged as audit events with admin user ID, action taken, and timestamp.
- Admin user search results must not expose user password hashes or tokens in any response.
- The activity log displayed must be paginated to prevent oversized responses and enumeration.
- Admin session termination capability (per-user and all-users) must be available (see V7.4.5).

**ASVS Mapping**: V8.1.1, V8.2.1, V8.3.1, V7.4.5, V16.1.1, V16.2.1, V16.2.5

**SSEM Implementation Notes**:
- **Analyzability**: All admin controller actions are in a dedicated `AdminController` with a single `[Authorize(Roles="Admin")]` attribute at the class level; authorization is not per-action.
- **Modifiability**: Admin-accessible data fields are defined by dedicated admin view models; adding or restricting fields requires only view model changes.
- **Testability**: All admin endpoints must return HTTP 403 when accessed by a non-admin authenticated user in integration tests.
- **Confidentiality**: User list responses include username, email, registration date, and note count only; password hashes, reset tokens, and session IDs are never included.
- **Accountability**: Every admin page view and search action is logged. Admin activity log is itself a secure audit asset with restricted write access.
- **Authenticity**: Admin role membership is verified from the authenticated session principal; a role claim in the request body is not honored.
- **Availability**: User search and activity log endpoints are paginated and rate-limited to prevent resource exhaustion by a compromised admin account.
- **Integrity**: Admin dashboard statistics (user count, note count) are computed by the server at request time; cached counts include a maximum staleness TTL.
- **Resilience**: Admin dashboard gracefully handles partial data availability (e.g., if activity log service is unavailable, the dashboard still renders user counts with a status indicator).

**FIASSE Tenet Annotations**:
- **S2.1**: Class-level `[Authorize(Roles="Admin")]` is not assumed to be the only admin protection; additional data-level checks (e.g., not returning all note content) apply even within admin context.
- **S2.2**: Paginated responses and rate limiting ensure the admin dashboard remains functional even if an admin account is compromised and used to enumerate data at high volume.
- **S2.3**: Comprehensive admin action logging enables detection of privilege misuse (insider threat or compromised admin account) through structured audit trails.
- **S2.4**: Class-level authorization attribute is a systemic control that prevents individual developers from accidentally leaving admin actions unprotected when adding new endpoints to the controller.
- **S2.6**: Admin audit logs with structured entries (who, what, when, which resource) are the primary observability mechanism for privilege-level activities in this application.

**Acceptance Criteria:**
- [ ] An authenticated non-admin user requesting any admin URL receives HTTP 403.
- [ ] An unauthenticated request to any admin URL receives HTTP 401 or redirect to login.
- [ ] User list API response does not contain password hash, session token, or reset token fields.
- [ ] Each admin page visit produces a log entry with admin user ID, URL accessed, and timestamp.
- [ ] Activity log view is paginated; requesting without pagination parameters returns only the first page.
- [ ] Admin can terminate the active sessions of a specific user.

---

### Feature F-14: User Profile Management

**Original REQ-014**: Users update username, email, and password. Changes saved to database.

**Additional Requirements Added:**
- Password change must require the current password in addition to the new password.
- New password must pass the same policy as registration (length, common-password check).
- After a successful password change, the user is offered the option to terminate all other active sessions.
- Username change must enforce uniqueness server-side.
- Email change must validate the new email format server-side.
- Profile update requests must include an anti-forgery token.
- Profile changes (username, email, password change) must be logged with user ID, field changed, and timestamp (not the new value for sensitive fields).

**ASVS Mapping**: V6.2.2, V6.2.3, V6.2.4, V7.4.3, V2.2.1, V16.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Profile update controller has explicit, named branches: `UpdateUsername`, `UpdateEmail`, `UpdatePassword`; each is a distinct action with its own validation logic, not a single method handling all field types.
- **Modifiability**: Password policy for profile updates reuses the same validation service as registration; updating the policy applies to both flows simultaneously.
- **Testability**: "Current password required for password change" is a mandatory integration test. Session termination after password change is verifiable.
- **Confidentiality**: New email addresses and new password values are not logged; the log records only that the field was changed and by whom.
- **Accountability**: All profile field changes logged with user ID, field name, and timestamp. Password change produces a distinct log entry.
- **Authenticity**: Current password verification re-authenticates the user before allowing a credential change.
- **Availability**: Profile update endpoint is not rate-limited as aggressively as login (authenticated user context), but abusive patterns (mass username changes) are detectable via logging.
- **Integrity**: Username uniqueness is enforced at both application and database constraint levels.
- **Resilience**: If the email service is unavailable during an email change (if confirmation is required), the old email remains valid until confirmation.

**FIASSE Tenet Annotations**:
- **S2.1**: Shared password policy validation between registration and profile ensures policy changes are applied uniformly, not just to new users.
- **S2.2**: Requiring current password for credential changes prevents account takeover via CSRF or session hijacking from automatically changing credentials.
- **S2.3**: Offering session termination after password change gives users a direct mechanism to limit the impact of a compromised session.
- **S2.4**: Reusing the registration password validation service is an engineering reuse pattern that prevents the profile update path from being a weaker credential entry point.
- **S2.6**: Field-change logging without logging values creates an audit trail that supports incident investigations (was this account modified by the attacker?) without exposing new credential values.

**Acceptance Criteria:**
- [ ] A password change request without the current password returns HTTP 400; password is unchanged.
- [ ] A password change request where the new password matches a top-3000 entry returns a validation error.
- [ ] After a successful password change, the user is presented with an option to log out all other sessions.
- [ ] Changing the username to an already-used username returns a conflict error; no change is made.
- [ ] Email format validation rejects malformed email addresses server-side.
- [ ] Profile change events appear in logs with user ID, field name, and timestamp.

---

### Feature F-15: Top Rated Notes

**Original REQ-015**: Public notes sorted by average rating (descending) with minimum 3 ratings. Shows title, author, average rating, count, and preview.

**Additional Requirements Added:**
- Top-rated endpoint must be rate-limited per IP to prevent scraping of public content at high volume.
- Note previews (first N characters of content) must be output-encoded before rendering.
- The average rating displayed must be computed server-side; it must not be derived from a client-supplied parameter.
- Only notes that remain public at query time are included; the filter is applied in the database query, not in application memory.

**ASVS Mapping**: V8.2.2, V8.3.1, V2.4.1

**SSEM Implementation Notes**:
- **Analyzability**: Top-rated query is a named repository method; visibility filter and minimum-ratings filter are both applied in the query predicate.
- **Modifiability**: Minimum rating count (currently 3) is a configuration value; changing it requires no code deployment.
- **Testability**: Integration test verifies that a private note with high ratings does not appear in the top-rated list after its visibility is changed.
- **Confidentiality**: Author field displays the username only; no user email or user ID is exposed.
- **Accountability**: No individual security logging required for read-only public endpoint; rate-limit breaches are logged.
- **Authenticity**: No authentication required for this endpoint. Author identity is the stored username at time of query.
- **Availability**: Database query is indexed on `IsPublic`, `RatingCount`, and `AverageRating` for efficient sorting at scale.
- **Integrity**: Output encoding of note previews prevents XSS via crafted note content appearing in the top-rated list.
- **Resilience**: Endpoint returns a cached result with a short TTL (e.g., 5 minutes) to reduce database load; cache is invalidated on rating changes.

**FIASSE Tenet Annotations**:
- **S2.1**: Database-level visibility filter ensures a note that was public when it earned ratings but is later made private is correctly excluded, without any code change.
- **S2.2**: Caching with short TTL balances data freshness with database load, keeping the feature valuable without degrading performance.
- **S2.3**: Rate limiting on a public, unauthenticated endpoint prevents it from becoming a content enumeration surface for automated scrapers.
- **S2.4**: Output encoding of previews is a rendering-layer control that protects against XSS regardless of what note content authors submit.
- **S2.6**: Rate-limit breach events provide operational visibility into automated access patterns on the public endpoint.

**Acceptance Criteria:**
- [ ] A note that is private does not appear in the top-rated list, even if it has high ratings and the correct count.
- [ ] A note with fewer than 3 ratings does not appear in the top-rated list.
- [ ] Note previews containing HTML tags are rendered as escaped text.
- [ ] More than 60 requests per minute from the same IP to the top-rated endpoint triggers a rate-limit response.

---

### Feature F-16: Note Ownership Reassignment

**Original REQ-016**: Admins change the owner of any note to a different existing user.

**Additional Requirements Added:**
- The reassignment action must verify the target user exists server-side before executing the update.
- Reassignment must be logged with admin user ID, note ID, previous owner user ID, new owner user ID, and timestamp.
- Reassignment must be atomic; if the database update fails, the note retains its original owner.
- Associated share links and ratings must be reviewed for consistency after reassignment; the PRD must explicitly define whether share links carry over (recommended: yes) or are revoked.
- Admins must confirm the reassignment (CSRF-protected form POST; GET-only requests must not execute reassignment).

**ASVS Mapping**: V8.2.1, V8.3.1, V2.3.3, V16.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Reassignment is a single, atomic update to the note's `UserId` field; no cascade logic is required unless the PRD specifies share link revocation.
- **Modifiability**: If share link behavior on reassignment changes (e.g., revoke on reassign), the change is localized to the reassignment service method.
- **Testability**: Admin-only access, target user existence check, atomicity, and audit log presence are each a dedicated integration test.
- **Confidentiality**: The reassignment response does not expose the previous or new owner's personal details beyond their username.
- **Accountability**: Reassignment event is a privileged administrative action; the log entry includes all actors and targets with sufficient detail for a forensic timeline.
- **Authenticity**: Admin identity is taken from the authenticated session; the admin's own user ID is the actor recorded in the log.
- **Availability**: Reassignment is a low-frequency operation; no special availability controls beyond standard admin rate limiting are required.
- **Integrity**: Target user existence is validated before the update to prevent orphaned note ownership.
- **Resilience**: If the database update fails, the original ownership is preserved; the admin receives an error response.

**FIASSE Tenet Annotations**:
- **S2.1**: Explicit documentation of share-link behavior on reassignment prevents ambiguity that could lead to either over-sharing or unexpected access loss when the feature is implemented.
- **S2.2**: Atomicity ensures the reassignment either fully completes or has no effect; no partial state degrades content integrity.
- **S2.3**: Complete audit logging of ownership reassignment enables detection of privilege misuse (e.g., admin reassigning notes for personal gain) through log review.
- **S2.4**: POST-only enforcement with anti-forgery token prevents the reassignment from being triggered by a CSRF attack against an admin user's browser session.
- **S2.6**: The reassignment log entry (admin actor, note ID, old owner, new owner, timestamp) is the minimum complete record needed to reconstruct the full chain of custody for any note.

**Acceptance Criteria:**
- [ ] A GET request to the reassignment action URL does not execute the reassignment.
- [ ] A reassignment request without an anti-forgery token returns HTTP 400.
- [ ] Reassigning a note to a non-existent user ID returns a validation error; the note owner is unchanged.
- [ ] Successful reassignment produces a log entry with admin user ID, note ID, previous owner ID, new owner ID, and timestamp.
- [ ] If the database update fails, the note's owner remains the original user; the admin receives an error message.
- [ ] A non-admin authenticated user requesting the reassignment endpoint receives HTTP 403.

---

## 3. Global Securability Requirements

These requirements apply across all features and are not specific to a single feature.

### GSR-01: HTTPS / Transport Security

- TLS 1.2 minimum, TLS 1.3 preferred, enforced on all endpoints. (ASVS V12.1.1, V12.2.1)
- Publicly trusted TLS certificate required. (ASVS V12.2.2)
- `Strict-Transport-Security` header on all responses with `max-age=31536000; includeSubDomains`. (ASVS V3.4.1)

### GSR-02: Security Headers

All HTML responses must include: (ASVS V3.4.3, V3.4.4, V3.4.5, V3.4.6)
- `Content-Security-Policy` with `object-src 'none'` and `base-uri 'none'` at minimum; nonces or allowlists for scripts.
- `X-Content-Type-Options: nosniff`
- `Referrer-Policy: strict-origin-when-cross-origin` or stricter.
- `frame-ancestors 'none'` in CSP (or equivalent).

### GSR-03: Anti-Forgery Tokens

All state-changing form submissions (POST, PUT, DELETE) must include and validate an anti-forgery token. (ASVS V3.5 ΓÇö Browser Origin Separation) Token validation is enforced at the framework level (ASP.NET Core `ValidateAntiForgeryToken` applied globally or per-controller class).

### GSR-04: Authorization Documentation

Authorization rules must be documented before implementation, covering: (ASVS V8.1.1)
- Which roles can access which controller actions.
- Which data records are accessible by which users (owner, admin, public).
- What fields are returned to each role in each context.

### GSR-05: Structured Security Logging

A logging inventory must define: (ASVS V16.1.1, V16.2.1, V16.2.2, V16.2.5)
- Which security events are logged and at which layer.
- Log format (structured JSON or equivalent).
- UTC timestamps on all log entries.
- Sensitive fields (passwords, tokens) that must never appear in logs.
- Retention policy for security logs.

Log entries for all security events must contain at minimum: `timestamp` (UTC), `event_type`, `user_id` (or anonymous), `source_ip`, `outcome`, and `resource_id` where applicable.

### GSR-06: Input Validation Baseline

All external inputs (form fields, query parameters, route parameters, headers used in business logic) must be validated server-side by default. (ASVS V2.2.1, V2.2.2) Client-side validation is a UX enhancement only; it is not a security control.

### GSR-07: Output Encoding

All user-supplied content rendered in Razor views must be encoded using Razor's default HTML encoding. Raw HTML rendering (`@Html.Raw`) must not be used for user-supplied content. (ASVS V3.2.2)

### GSR-08: Error Handling

Application errors must not expose stack traces, internal file paths, database schema details, or framework version information to end users. A generic error page is shown; detailed errors are written to the application log only. (ASVS V16 series)

---

## 4. Open Gaps and Assumptions

| ID | Gap / Assumption | Risk | Recommended Action |
|----|-----------------|------|-------------------|
| G-01 | Share link behavior on note deletion: PRD says deletion removes share links, but the behavior when a note is set private after a share link is created is underspecified. | Medium ΓÇö users may expect private notes to be inaccessible via old share links. | Define policy explicitly: share links grant access independent of visibility (link is the control); or, making a note private automatically revokes all share links. |
| G-02 | File download authentication: The PRD requires authentication for note access but does not specify whether file download endpoints require authentication or rely on the share link token. | High ΓÇö unauthenticated direct file download could bypass note visibility controls. | Require authentication or valid share token for all attachment download requests; direct file paths must not be guessable or browseable. |
| G-03 | Email confirmation for registration: The PRD does not require email confirmation after registration. | Medium ΓÇö unverified email addresses enable account enumeration and spam account creation. | For Level 2 assurance, email confirmation is recommended before the account becomes fully functional. |
| G-04 | Audit log write protection: The PRD mentions logging but does not specify that audit logs are write-once or append-only to prevent tampering. | High ΓÇö a compromised admin account could delete or modify audit logs. | Audit logs should be written to a separate, append-only store not directly accessible via the web application. |
| G-05 | Concurrent session management: The PRD does not define behavior when a user is logged in on multiple devices. | Low-Medium ΓÇö no limit on concurrent sessions increases the window of unauthorized access if a session token is stolen. | Define maximum concurrent sessions per user and behavior when the limit is exceeded (oldest session terminated or user notified). |
| G-06 | Password change requires re-authentication: The PRD mentions profile management but does not explicitly require the current password for email address changes (only for password changes). | Medium ΓÇö session hijacking could enable an attacker to change the email address and use the forgot-password flow for full takeover. | Require current password for email address changes, or require re-authentication (password prompt) before any credential-level change. |
| G-07 | File content scanning: Magic-byte validation catches type mismatches but does not detect malware embedded in valid file formats (e.g., malicious macros in DOCX). | Medium ΓÇö for an internal note-taking tool this is acceptable at Level 2; for external-facing environments it warrants a virus-scanning integration. | Document the decision: either accept the residual risk or integrate an anti-malware scanning service for uploaded files. |
| G-08 | Search index and full-text search configuration: The PRD references keyword search but does not specify the search mechanism (SQL LIKE, full-text index, external search service). The choice affects both performance and the risk of injection. | Medium ΓÇö SQL LIKE queries with unescaped wildcards can cause performance issues; parameterization requirements differ by search mechanism. | Explicitly specify the search implementation and verify that parameterization is applied regardless of mechanism. |

---

## 5. Revised Requirements Table

The following updates the original requirements table to reflect securability augmentations:

| Requirement ID | Description | User Story | Expected Behavior/Outcome | Securability Additions |
|----------------|-------------|------------|---------------------------|------------------------|
| REQ-001 | User Registration | As a visitor, I want to create an account. | Users register with username, email, and password. | Password min 8 chars (15 recommended), max ΓëÑ64; checked against top-3000 list; no composition rules; type=password; paste allowed; enumeration-safe errors; registration event logged. |
| REQ-002 | User Login | As a registered user, I want to log in. | Username/password login; cookie session; timeout on logout or expiry. | Session token CSPRNG ΓëÑ128 bits; regenerated on login; Secure+HttpOnly+SameSite=Strict cookie; inactivity timeout documented; absolute session lifetime documented; rate-limited; structured login events logged. |
| REQ-003 | Password Reset | As a user, I want to reset my password. | Time-limited token (1 hr) via email; used/expired tokens rejected. | Token CSPRNG ΓëÑ128 bits; single-use; all sessions terminated on reset; new password passes policy; rate-limited; enumeration-safe response; reset events logged. |
| REQ-004 | Note Creation | As a logged-in user, I want to create notes. | Authenticated users create titled, private notes linked to their ID. | Server-side field length limits; server-assigned default visibility; UserId from session only; anti-forgery token required; creation logged. |
| REQ-005 | File Attachment | As a user, I want to attach files to notes. | Upload PDF, DOC, DOCX, TXT, PNG, JPG, JPEG; stored with unique IDs; original filename in metadata. | Server-enforced size limit; magic-byte content validation; server-generated storage paths; Content-Disposition: attachment on download; per-user quota; upload events logged. |
| REQ-006 | Note Editing | As a note owner, I want to edit my notes. | Owners modify title, content, visibility; last-modified updated. | Server-side ownership check (IDOR prevention); anti-forgery token; same field validation as creation; edit events logged. |
| REQ-007 | Note Deletion | As a note owner, I want to delete my notes. | Owners/admins delete notes; cascade deletes attachments, ratings, share links. | Atomic cascade deletion (transaction); admin deletions logged separately; storage and database deletion coordinated; share links for deleted notes return 404/410. |
| REQ-008 | Note Sharing | As a note owner, I want to share notes via link. | Unique share links; anyone with link can view without auth; links regenerable/revocable. | Token CSPRNG ΓëÑ128 bits; revocation immediate server-side; share view response excludes PII; link events logged; share endpoint rate-limited. |
| REQ-009 | Public/Private Notes | As a note owner, I want to control note visibility. | Toggle public/private; public notes searchable; private notes owner-only. | Visibility enforced at query layer; fail-safe default to private; visibility changes logged. |
| REQ-010 | Note Rating | As a user, I want to rate notes. | Authenticated users rate 1ΓÇô5 stars; ratings visible to all; users edit own rating. | Server-side range validation (1ΓÇô5 integer); note visibility/existence check; per-user rate limiting; rating events logged. |
| REQ-011 | Rating Management | As a note owner, I want to see my note ratings. | Owners view all ratings: value, comment, username, timestamp; average displayed. | Ownership enforced server-side; rater email excluded from response; average computed server-side; paginated. |
| REQ-012 | Note Search | As a user, I want to search notes by keywords. | Keyword search; owned notes (any visibility) and public notes; private notes from others excluded. | Visibility filter at database query layer; search query length limit; rate-limited; output-encoded excerpts; authentication required. |
| REQ-013 | Admin Dashboard | As an admin, I want to manage users and content. | Admins view stats, user list, activity log; search users. | Class-level [Authorize(Roles="Admin")]; no PII beyond username/email/dates; all admin actions logged; paginated responses; admin session termination capability. |
| REQ-014 | User Profile Management | As a user, I want to edit my profile. | Users update username, email, password. | Current password required for password change; new password passes policy; option to terminate other sessions after credential change; anti-forgery token; profile change events logged. |
| REQ-015 | Top Rated Notes | As a user, I want to see highest-rated notes. | Public notes sorted by average rating; min 3 ratings; title, author, rating, count, preview. | Rate-limited per IP; output-encoded previews; visibility filter at query layer; minimum-rating filter in query. |
| REQ-016 | Note Ownership Reassignment | As an admin, I want to reassign notes. | Admins change note owner to different existing user. | POST-only with anti-forgery token; target user existence validated; atomic update; reassignment logged with all actors and note ID; admin-only enforced. |

---

## 6. Conclusion

This enhanced PRD augments the Loose Notes application's functional requirements with ASVS Level 2 security requirements, nine-attribute SSEM implementation guidance, and FIASSE foundational tenet annotations for every feature. Delivery teams should treat the acceptance criteria in each enhanced feature specification as testable security requirements, not optional hardening. The open gaps in Section 4 must each receive an explicit disposition decision before implementation begins.
