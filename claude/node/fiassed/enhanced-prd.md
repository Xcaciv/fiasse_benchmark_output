I'll execute the play using my built-in knowledge of FIASSE/SSEM and OWASP ASVS, since the data files referenced in CLAUDE.md are not present in this working directory.

---

# Project Requirements Document ΓÇö Loose Notes Web Application (Securability-Enhanced)

**Framework**: ASP.NET Core MVC | **Database**: EF Core (relational) | **Auth**: ASP.NET Core Identity | **Storage**: Local/cloud file system | **Frontend**: Razor Views + JavaScript

---

## ASVS Level Decision

**Selected Level: ASVS Level 2**

**Rationale:**

This is a production multi-user web application with authenticated users, personal data (email addresses, usernames), user-generated content, file upload/download, public note sharing via unauthenticated share links, and administrative privilege operations. These characteristics place it firmly in the Level 2 category.

Level 1 is insufficient because:
- The application stores personally identifiable information (email address, username)
- File upload attack surface requires validated boundary controls beyond baseline
- Share-link access without authentication introduces an unauthenticated trust boundary crossing that requires deliberate controls
- Administrative operations (note reassignment, user management) require auditable accountability beyond ad hoc logging

Level 3 is not warranted at baseline because:
- No highly sensitive regulated data (PHI, financial instruments) is described
- No formal compliance mandate (PCI-DSS, HIPAA) is indicated
- Attacker interest profile is moderate

**Level Escalation Notes:**
- REQ-005 (File Attachment) and REQ-008 (Note Sharing) have localized characteristics warranting Level 2+ diligence on file content validation and unauthenticated access paths respectively.
- REQ-013 (Admin Dashboard) and REQ-016 (Note Ownership Reassignment) require full Level 2 access control and audit coverage.

---

## Feature-ASVS Coverage Matrix

| Feature | ASVS Section | Requirement ID | Level | Coverage | PRD Change Needed |
|---------|-------------|----------------|-------|----------|-------------------|
| REQ-001 | V2.1 Password Security | 2.1.1ΓÇô2.1.12 | 2 | Partial | Add explicit password complexity policy and breach-corpus check |
| REQ-001 | V5.1 Input Validation | 5.1.1ΓÇô5.1.4 | 2 | Missing | Add server-side input validation for all registration fields |
| REQ-001 | V8.3 Sensitive Private Data | 8.3.4 | 2 | Missing | Add data minimization policy ΓÇö collect only necessary fields |
| REQ-002 | V2.2 General Auth | 2.2.1ΓÇô2.2.2 | 2 | Partial | Add account lockout after N failed attempts; add re-auth for sensitive operations |
| REQ-002 | V3.2 Session Binding | 3.2.1ΓÇô3.2.4 | 2 | Missing | Specify session token entropy, regeneration on privilege change |
| REQ-002 | V3.3 Session Logout | 3.3.1ΓÇô3.3.4 | 2 | Missing | Specify absolute session timeout (e.g., 8 hours), idle timeout |
| REQ-002 | V7.2 Log Processing | 7.2.1ΓÇô7.2.2 | 2 | Partial | Specify structured log schema for auth events |
| REQ-003 | V2.5 Credential Recovery | 2.5.1ΓÇô2.5.7 | 2 | Partial | Require single-use token; add notification to registered email on reset |
| REQ-003 | V2.1 Password Security | 2.1.1 | 2 | Missing | Enforce same password policy at reset as at registration |
| REQ-004 | V4.1 General Access Control | 4.1.1ΓÇô4.1.5 | 2 | Partial | Require server-side ownership assertion, not just client-side note linkage |
| REQ-004 | V5.2 Sanitization | 5.2.1ΓÇô5.2.3 | 2 | Missing | Specify HTML sanitization policy for note content; define max length |
| REQ-004 | V8.2 Client-Side Data Protection | 8.2.2 | 2 | Missing | Prevent browser caching of private note content |
| REQ-005 | V12.1 File Upload | 12.1.1ΓÇô12.1.3 | 2 | Partial | Add MIME type validation (not just extension); add max file size; store outside web root |
| REQ-005 | V12.3 File Execution | 12.3.1ΓÇô12.3.3 | 2 | Missing | Require antivirus/content inspection or sandboxed processing; prevent path traversal |
| REQ-005 | V4.2 Operation-Level Access Control | 4.2.1ΓÇô4.2.2 | 2 | Missing | Confirm only note owner (or admin) can attach files; enforce server-side |
| REQ-006 | V4.1 General Access Control | 4.1.3 | 2 | Partial | Server-side ownership verified before any update is persisted |
| REQ-006 | V5.1 Input Validation | 5.1.3ΓÇô5.1.4 | 2 | Missing | Validate all modified fields server-side; canonicalize before storage |
| REQ-007 | V4.1 General Access Control | 4.1.3 | 2 | Covered | Ownership/admin check already described |
| REQ-007 | V11.1 Business Logic | 11.1.4ΓÇô11.1.5 | 2 | Missing | Add cascading delete order specification; add undo/recovery consideration |
| REQ-008 | V3.4 Cookie-Based Session | 3.4.1ΓÇô3.4.5 | 2 | Missing | Share tokens must be cryptographically random; define token entropy minimum |
| REQ-008 | V4.1 Access Control | 4.1.1 | 2 | Partial | Confirm share link does not expose metadata beyond intended note content |
| REQ-008 | V8.1 Data Protection | 8.1.1 | 2 | Missing | Share links must not expose user PII to unauthenticated viewers |
| REQ-009 | V4.1 Access Control | 4.1.2ΓÇô4.1.3 | 2 | Covered | Public/private toggle and enforcement described |
| REQ-009 | V4.3 Other Access Control | 4.3.1 | 2 | Missing | Specify that visibility changes are audit-logged |
| REQ-010 | V4.2 Operation-Level Access Control | 4.2.1 | 2 | Partial | Confirm one-rating-per-user-per-note enforcement server-side |
| REQ-010 | V11.1 Business Logic | 11.1.1 | 2 | Missing | Prevent self-rating; define allowed value range server-side |
| REQ-011 | V4.1 Access Control | 4.1.3 | 2 | Partial | Confirm only note owner can view rating list |
| REQ-012 | V5.3 Output Encoding | 5.3.1ΓÇô5.3.3 | 2 | Partial | Confirm search query results are output-encoded before rendering |
| REQ-012 | V5.1 Input Validation | 5.1.1 | 2 | Missing | Validate and bound search query length server-side to prevent DoS |
| REQ-013 | V4.3 Admin Access Control | 4.3.1ΓÇô4.3.3 | 2 | Partial | Add requirement for admin sessions to have shorter timeout; require re-auth for destructive actions |
| REQ-013 | V7.2 Log Processing | 7.2.1 | 2 | Covered | Activity log already described |
| REQ-014 | V2.1 Password Security | 2.1.1 | 2 | Missing | Enforce password policy on change; require current password confirmation |
| REQ-014 | V2.5 Credential Recovery | 2.5.6 | 2 | Missing | Send notification to old email on email address change |
| REQ-015 | V4.1 Access Control | 4.1.1 | 2 | Covered | Only public notes appear in top rated |
| REQ-015 | V11.1 Business Logic | 11.1.2 | 2 | Missing | Define anti-gaming constraint: minimum distinct raters, not just minimum count |
| REQ-016 | V4.3 Admin Access Control | 4.3.1 | 2 | Covered | Admin-only operation |
| REQ-016 | V7.2 Log Processing | 7.2.1 | 2 | Missing | Require structured audit log entry with before/after owner, actor, timestamp |

---

## Enhanced Feature Specifications

---

### Feature REQ-001: User Registration

**Original**: Users can register with username, email address, and password.

**Augmented Requirements**:
- Passwords must meet minimum complexity: ΓëÑ12 characters, no maximum truncation (ASVS 2.1.1ΓÇô2.1.2).
- Passwords must be checked against a known-breached password list (HIBP API or equivalent) before acceptance (ASVS 2.1.7).
- All registration fields must be validated server-side for type, length, and format before persistence.
- Registration must collect only username, email, and password (no surplus PII).
- Duplicate username and email must be detected with a neutral error message that does not confirm or deny account existence.
- Registration form must include CSRF anti-forgery token.

**ASVS Mapping**: V2.1.1, V2.1.2, V2.1.7, V5.1.1, V5.1.3, V8.3.4

**SSEM Implementation Notes**:
- **Analyzability**: Registration service should be a discrete, named component (e.g., `UserRegistrationService`) with a single responsibility; avoid embedding registration logic in the controller.
- **Modifiability**: Password policy should be configured externally (e.g., `IdentityOptions`) so requirements can be updated without code changes.
- **Testability**: Registration flow must be testable with boundary inputs (min/max length, invalid email formats, duplicate registration) via unit and integration tests.
- **Confidentiality**: Passwords are never stored in plaintext; PBKDF2 via ASP.NET Core Identity is required. Email is stored as PII ΓÇö apply field-level access minimization.
- **Accountability**: Log successful registration with user ID and timestamp; log failed registration attempts with reason code (not password value).
- **Authenticity**: Email address must be confirmed (verification link) before account is considered active and trusted for login.
- **Availability**: Registration endpoint must be rate-limited (e.g., max 10 requests/IP/hour) to prevent mass account creation abuse.
- **Integrity**: Canonical normalization of email (lowercase, trimmed) must occur server-side before storage to prevent duplicate accounts via case variation.
- **Resilience**: Failed password hashing or database write must fail closed (return error, not partial account creation).

**FIASSE Tenet Annotations**:
- S2.1: Registration controls must be configurable to adapt as credential theft patterns evolve ΓÇö do not hardcode password rules in controller logic.
- S2.2: The registration pathway should degrade gracefully under load; if the breach-check API is unavailable, fail to a conservative default (reject or queue for later check) without blocking registration indefinitely.
- S2.3: Email verification before activation reduces the probability of account enumeration, credential stuffing, and spam account creation at scale.
- S2.4: Enforce breach-corpus checking and server-side canonicalization as systematic controls rather than relying on UI hints alone.
- S2.6: Emit structured log events for registration outcomes so audit pipelines can detect abnormal registration bursts.

**Updated Acceptance Criteria**:
- [ ] Passwords shorter than 12 characters are rejected with a clear validation message.
- [ ] Passwords present in the HaveIBeenPwned (or equivalent) breach corpus are rejected.
- [ ] Submitting registration with an already-used email returns a neutral message ("if this email is not registered, you will receive a confirmation").
- [ ] Unverified accounts cannot log in.
- [ ] Registration endpoint returns HTTP 429 after 10 attempts from the same IP within one hour.
- [ ] Successful registration emits a structured log entry containing user ID and timestamp (no password).

---

### Feature REQ-002: User Login / Authentication

**Original**: Users log in with username and password; successful auth creates a session cookie.

**Augmented Requirements**:
- Account lockout must be enforced after 5 consecutive failed attempts; lockout duration must be configurable (default 15 minutes) (ASVS 2.2.4).
- Session tokens must be regenerated upon successful authentication (ASVS 3.2.1).
- Session cookie must use Secure, HttpOnly, and SameSite=Strict (or Lax) attributes (ASVS 3.4.1ΓÇô3.4.3).
- Idle timeout: sessions expire after 30 minutes of inactivity. Absolute timeout: 8 hours regardless of activity (ASVS 3.3.1ΓÇô3.3.2).
- Login page must be served exclusively over HTTPS (ASVS 9.1.1).
- Failed login attempts must be logged with username (not password) and source IP (ASVS 7.2.1).
- Generic error message on failure: "Invalid username or password" ΓÇö do not differentiate between unknown user and wrong password.

**ASVS Mapping**: V2.2.1, V2.2.4, V3.2.1, V3.3.1, V3.3.2, V3.4.1, V3.4.2, V3.4.3, V7.2.1, V9.1.1

**SSEM Implementation Notes**:
- **Analyzability**: Lockout logic must be centralized in a named middleware or service ΓÇö not scattered across action methods. Lockout state must be observable in admin tooling.
- **Modifiability**: Lockout thresholds and session timeout values must be driven by `appsettings.json` configuration, not magic numbers.
- **Testability**: Login flow must include integration tests for lockout progression, session token regeneration, and cookie attribute assertions.
- **Confidentiality**: Session tokens must have ΓëÑ128 bits of entropy (ASVS 3.2.4). Login response must not echo back the submitted password in any form.
- **Accountability**: Every authentication event (success or failure) must produce a structured log entry: `{ event: "auth.login", outcome: "success|failure", userId/username, ip, timestamp }`.
- **Authenticity**: Session binding to user identity must be server-side only; client-supplied user identity claims in cookies must not be trusted without cryptographic validation.
- **Availability**: Lockout must not be exploitable for user DoS; consider CAPTCHA or progressive delay as an alternative to full account lock for distributed attacks.
- **Integrity**: Session token must be invalidated server-side on logout ΓÇö cookie deletion alone is insufficient.
- **Resilience**: Authentication service must handle transient database failures with a secure fail-closed behavior (deny access, not bypass auth).

**FIASSE Tenet Annotations**:
- S2.1: Lockout policy and timeout values must be externally configurable ΓÇö threat patterns change, and hardcoded values cannot adapt.
- S2.2: Login must remain available under credential-stuffing load; rate limiting and progressive lockout are preferable to full-account lockout for distributed attacks.
- S2.3: Credential stuffing is a material threat; consistent error messages and lockout together reduce the probability of mass account compromise.
- S2.4: Generic error messaging and server-side session management are systematic controls that scale across all user accounts rather than per-user mitigations.
- S2.6: Structured authentication event logs enable downstream threat detection (SIEM correlation) without embedding investigative logic in the application.

**Updated Acceptance Criteria**:
- [ ] After 5 consecutive failed login attempts, account is locked for 15 minutes; subsequent attempts return a lockout message.
- [ ] On successful login, a new session token is issued (previous token is invalidated).
- [ ] Session cookie has Secure, HttpOnly, SameSite=Lax (or Strict) attributes verifiable via browser dev tools.
- [ ] Session is invalidated after 30 minutes of inactivity.
- [ ] Session is invalidated after 8 hours regardless of activity.
- [ ] All login attempts (success and failure) produce a structured log entry with outcome, username, IP, and timestamp.

---

### Feature REQ-003: Password Reset

**Original**: Users request reset via email; system sends time-limited token (1 hour) to set new password.

**Augmented Requirements**:
- Reset tokens must be cryptographically random with ΓëÑ128 bits of entropy (ASVS 2.5.6).
- Token must be single-use ΓÇö invalidated immediately upon first use or expiry (ASVS 2.5.7).
- User must be notified at their registered email when a password reset is *requested* (not only when completed), to alert against unauthorized reset attempts.
- New password submitted at reset must comply with the same policy as registration (ΓëÑ12 chars, breach corpus check).
- Reset token must not be logged in application logs.
- The reset form must include a CSRF anti-forgery token.
- Account is not locked and the reset email dispatch must return a neutral response to prevent user enumeration.

**ASVS Mapping**: V2.5.1, V2.5.3, V2.5.6, V2.5.7, V2.1.1, V2.1.7

**SSEM Implementation Notes**:
- **Analyzability**: Token generation and validation must be encapsulated in a dedicated `PasswordResetTokenService` ΓÇö not inline in the controller.
- **Modifiability**: Token expiry and entropy settings must be externally configurable.
- **Testability**: Token expiry, single-use enforcement, and breach-corpus rejection must each have targeted test cases.
- **Confidentiality**: Reset tokens must be hashed before storage (only hash in DB; plaintext sent to user via email) ΓÇö analogous to password storage.
- **Accountability**: Log reset request (email address, IP, timestamp) and reset completion (user ID, IP, timestamp); do not log token values.
- **Authenticity**: Token validation must be constant-time to prevent timing-based token enumeration.
- **Availability**: Reset email delivery failures must not expose whether an account exists; failures should be logged for operator visibility.
- **Integrity**: Used tokens must be deleted or invalidated atomically with the password update (no TOCTOU window).
- **Resilience**: If email dispatch fails, inform the user to retry without exposing internal error details; retry logic must be bounded.

**FIASSE Tenet Annotations**:
- S2.1: Token parameters (expiry, entropy) must be configurable to respond to evolving credential-theft patterns.
- S2.2: Recovery flow must remain available even if email delivery is degraded; log failures for operator review without blocking the user indefinitely.
- S2.3: Single-use tokens and pre-reset notification together reduce the window of opportunity for unauthorized password takeover.
- S2.4: Hashing reset tokens before storage is a systematic control ΓÇö it limits blast radius if the database is compromised.
- S2.6: Structured log events for reset requests enable detection of targeted account-takeover campaigns.

**Updated Acceptance Criteria**:
- [ ] Requesting reset for a non-existent email returns the same neutral response as a valid email.
- [ ] Reset token is invalidated after first successful use; reusing the same link returns an error.
- [ ] Reset token expires after 1 hour; expired tokens are rejected.
- [ ] New password submitted at reset is validated against the same policy as registration.
- [ ] Notification email is sent upon reset *request* (not only on completion).
- [ ] Reset token value does not appear in any application log.

---

### Feature REQ-004: Note Creation

**Original**: Authenticated users create notes with title and content; notes default to private.

**Augmented Requirements**:
- Server-side validation must enforce: title non-empty, max 500 characters; content non-empty, max 100,000 characters (or a defined limit).
- Note content must be sanitized server-side to remove active/dangerous HTML before storage ΓÇö define sanitization policy (e.g., allow plain text only, or a safe HTML subset via an allowlist library).
- Note creation must assert authenticated identity server-side; `userId` must be derived from the server-side session, not from any client-submitted field.
- Creation timestamp must be set server-side (not client-provided).
- Notes default to private ΓÇö visibility must be explicitly changed by the owner.
- Browser caching of private note content must be suppressed (`Cache-Control: no-store`).

**ASVS Mapping**: V4.1.1, V4.1.3, V5.1.1, V5.1.3, V5.2.1, V5.3.3, V8.2.2

**SSEM Implementation Notes**:
- **Analyzability**: Note entity creation must flow through a `NoteService` with clearly named methods; controller must not contain validation or sanitization logic.
- **Modifiability**: Sanitization policy (allowed HTML elements) should be configurable or driven by a dedicated sanitization library (e.g., HtmlSanitizer), not hand-rolled regex.
- **Testability**: Sanitization behavior (script-tag stripping, allowed elements) must be unit-testable in isolation.
- **Confidentiality**: Private note content must carry `Cache-Control: no-store` headers; note IDs in URLs must not be sequential integers (use GUIDs or opaque identifiers to prevent enumeration).
- **Accountability**: Log note creation events: `{ event: "note.created", noteId, userId, visibility, timestamp }`.
- **Authenticity**: `ownerId` is always set from the authenticated session ΓÇö any client-supplied owner field must be ignored.
- **Availability**: Enforce content length limits to prevent storage exhaustion; return HTTP 413 for oversized payloads.
- **Integrity**: All input must be canonicalized (Unicode normalization, whitespace trimming) before storage to prevent homograph attacks on search.
- **Resilience**: Database write failure must not result in partial note creation; use transactions.

**FIASSE Tenet Annotations**:
- S2.1: Sanitization policy must be library-driven and updatable ΓÇö hand-rolled regex cannot adapt to new attack vectors.
- S2.2: Content length limits and database transactions protect availability and data consistency under adversarial or accidental load.
- S2.3: Server-side ownership binding and private-by-default visibility together minimize the blast radius of misconfiguration or UI errors.
- S2.4: Stripping dangerous HTML at ingestion (not just at render) is a systematic control that protects all downstream consumers of note data.
- S2.6: Note creation log events enable audit trails that support both security monitoring and data governance.

**Updated Acceptance Criteria**:
- [ ] Submitting a note with an empty title or content is rejected with a validation error.
- [ ] Notes created via API/form with a client-supplied `userId` field ignore that field; server session identity is used.
- [ ] Note content containing `<script>` tags is sanitized before storage; rendered output does not execute scripts.
- [ ] Newly created notes have `visibility = private` in the database by default.
- [ ] Private note detail pages return `Cache-Control: no-store` header.
- [ ] Creation event is emitted to the structured log.

---

### Feature REQ-005: File Attachment

**Original**: Users upload files (PDF, DOC, DOCX, TXT, PNG, JPG, JPEG) to notes; stored with unique identifiers.

**Augmented Requirements**:
- File type validation must verify both file extension AND MIME type (magic bytes / content sniffing), not extension alone (ASVS 12.1.1).
- Maximum file size limit must be defined and enforced server-side before content is written to disk (ASVS 12.1.2).
- Uploaded files must be stored outside the web root (not directly served by the application server) (ASVS 12.3.1).
- Files must be served via a controller action that enforces authentication and ownership checks, not via static file middleware.
- Stored filenames must be server-generated (UUID/GUID); original filename stored only in metadata ΓÇö never used in file system path.
- Path traversal must be explicitly prevented: reject filenames containing `..`, `/`, `\`, or null bytes.
- Only note owners (and admins) may attach files; this must be enforced server-side before storage.
- Total attachment storage per user should have a configurable quota limit.

**ASVS Mapping**: V12.1.1, V12.1.2, V12.1.3, V12.3.1, V12.3.3, V4.1.3, V4.2.1

**SSEM Implementation Notes**:
- **Analyzability**: File upload logic must be in a dedicated `FileStorageService` with a well-defined interface; upload, validation, and retrieval must be separately named methods.
- **Modifiability**: Allowed MIME types and size limits must be configurable via `appsettings.json`; adding new file types must not require code changes.
- **Testability**: MIME validation bypass attempts (spoofed extension, polyglot files) must have targeted test cases; path traversal inputs must be tested.
- **Confidentiality**: Stored files must not be accessible via predictable or guessable URLs; GUIDs for file identifiers, served only through authenticated download endpoints.
- **Accountability**: Log every file upload and download: `{ event: "attachment.uploaded|downloaded", fileId, noteId, userId, size, mimeType, timestamp }`.
- **Authenticity**: Serving controller must re-verify ownership/share-link validity on every download request ΓÇö do not rely on the upload-time check alone.
- **Availability**: Enforce per-user storage quotas; return HTTP 413 for oversized files; reject uploads that would exceed quota.
- **Integrity**: File content must be validated (MIME sniffing) before acceptance; malformed file headers must be rejected.
- **Resilience**: If storage write fails (disk full, I/O error), the attachment record must not be created in the database; use transactional atomicity.

**FIASSE Tenet Annotations**:
- S2.1: Allowed file types and size limits must be configurable ΓÇö threat surface of file uploads evolves as new attack techniques emerge.
- S2.2: Storage quotas and size limits protect availability for all users; a single user cannot exhaust storage.
- S2.3: Storing files outside the web root and serving via authenticated endpoints eliminates the direct-object-reference attack class.
- S2.4: MIME-type sniffing is a systematic control that cannot be bypassed by client-side extension manipulation.
- S2.6: Upload and download events in structured logs enable detection of bulk exfiltration patterns.

**Updated Acceptance Criteria**:
- [ ] Uploading a file with a `.php` extension renamed to `.jpg` (or similar polyglot) is rejected by MIME-type validation.
- [ ] Files larger than the configured maximum (e.g., 10 MB) are rejected before being written to disk.
- [ ] Attempting to download a file belonging to another user's private note returns HTTP 403.
- [ ] File path in storage is a UUID; original filename is only in the database metadata.
- [ ] A filename containing `../` in the request is sanitized/rejected without error disclosure.
- [ ] Upload event is emitted to the structured log with file size and MIME type.

---

### Feature REQ-006: Note Editing

**Original**: Note owners can modify title, content, and visibility; last-modified timestamp updated.

**Augmented Requirements**:
- Ownership must be verified server-side on every edit request ΓÇö not just on page load.
- All modified fields must be validated server-side with the same rules as note creation.
- Visibility changes (privateΓåÆpublic) must be audit-logged.
- Last-modified timestamp must be set server-side (not client-provided).
- Optimistic concurrency (e.g., ETag or row version) should be implemented to prevent lost-update race conditions.
- CSRF anti-forgery token required on all edit forms.

**ASVS Mapping**: V4.1.1, V4.1.3, V5.1.1, V5.1.3, V5.2.1, V11.1.6

**SSEM Implementation Notes**:
- **Analyzability**: Edit and create paths should share the same validation and sanitization service, not duplicate logic.
- **Modifiability**: Visibility state transitions should be modeled explicitly (e.g., a `NoteVisibility` enum) to make business rule changes isolated.
- **Testability**: Test must confirm that user B cannot edit user A's note even with a valid session.
- **Confidentiality**: Edit responses for private notes must include `Cache-Control: no-store`.
- **Accountability**: Log edit events with delta information: `{ event: "note.edited", noteId, userId, fieldsChanged, timestamp }`.
- **Authenticity**: The note ID in the URL must be cross-referenced with the authenticated session's userId server-side ΓÇö insecure direct object reference (IDOR) must be prevented.
- **Availability**: Concurrent edit conflict handling should degrade gracefully ΓÇö return a meaningful conflict error rather than silently overwriting.
- **Integrity**: Sanitization must be re-applied on every edit submission, not assumed from initial creation.
- **Resilience**: Failed edit persistence must not leave the note in a partially updated state; wrap in a database transaction.

**FIASSE Tenet Annotations**:
- S2.1: Server-side re-validation on every edit ensures controls are not bypassed by state manipulation between page load and form submit.
- S2.2: Optimistic concurrency prevents silent data loss under concurrent edit conditions.
- S2.3: Audit logging visibility changes reduces the impact of inadvertent data exposure by enabling post-hoc detection.
- S2.4: IDOR prevention through server-side ownership check is a systematic control that covers all notes, not just the ones the UI surfaces.
- S2.6: Structured edit events with field-change delta enable compliance auditing and anomaly detection.

**Updated Acceptance Criteria**:
- [ ] Authenticated user B submitting an edit for user A's note (by guessing note ID) receives HTTP 403.
- [ ] Submitting an edit form with an invalid (expired) CSRF token returns HTTP 400/403.
- [ ] Changing note visibility from private to public creates an audit log entry.
- [ ] Last-modified timestamp reflects server time, not any client-submitted value.

---

### Feature REQ-007: Note Deletion

**Original**: Note owners or admins can delete notes; system confirms before deletion; cascading delete of attachments, ratings, share links.

**Augmented Requirements**:
- Server-side ownership or admin-role check must occur before deletion, independent of UI confirmation.
- Cascading delete order must be defined: share links ΓåÆ attachments (files from storage) ΓåÆ ratings ΓåÆ note record ΓÇö to prevent orphaned storage artifacts.
- Physical file deletion from storage must be transactionally coupled with database record deletion; if storage delete fails, database record must not be removed (or a cleanup job must be defined).
- Deletion must be logged: `{ event: "note.deleted", noteId, originalOwnerId, actorId, actorRole, timestamp }`.
- CSRF anti-forgery token required on the delete confirmation form/action.
- Admins deleting notes not owned by them must produce a separate audit log entry.

**ASVS Mapping**: V4.1.3, V4.2.1, V7.2.1, V11.1.4

**SSEM Implementation Notes**:
- **Analyzability**: Cascading delete must be implemented in a named service method, not as a series of controller-level queries.
- **Modifiability**: Cascade order and storage cleanup should be configurable for soft-delete vs. hard-delete behavior in future.
- **Testability**: Integration test must confirm all cascaded records (attachments, ratings, share links) are removed after deletion.
- **Confidentiality**: After deletion, note content must not be recoverable through cache or storage artifacts without an explicit backup restore.
- **Accountability**: Deletion log must include both actor identity and original owner identity for cross-user admin deletes.
- **Authenticity**: The delete endpoint must validate the anti-forgery token and re-check authorization ΓÇö not rely solely on the confirmation page having been loaded.
- **Availability**: Deletion should not lock tables for extended periods; use batched deletes for notes with large attachment sets.
- **Integrity**: Orphaned file artifacts (storage files with no DB record) must be addressed ΓÇö either via transactional delete or a scheduled cleanup job.
- **Resilience**: If storage file deletion fails, the database record must be tombstoned or flagged for retry ΓÇö not silently left inconsistent.

**FIASSE Tenet Annotations**:
- S2.1: Soft-delete capability should be considered to allow recovery from accidental deletion ΓÇö hard deletes are irreversible.
- S2.2: Cascading deletion must be designed to handle large note sets without causing availability degradation (table locks, timeouts).
- S2.3: Complete cascade eliminates orphaned data that could become a compliance or privacy liability.
- S2.4: Server-side authorization on the DELETE action is non-negotiable ΓÇö UI confirmation does not constitute a security control.
- S2.6: Admin-performed deletions must produce distinct audit events traceable to the acting administrator.

**Updated Acceptance Criteria**:
- [ ] User B cannot delete user A's note even by crafting a DELETE request with user A's note ID.
- [ ] After note deletion, querying for associated attachments, ratings, and share links returns no results.
- [ ] Files associated with deleted attachments are removed from storage (or flagged for cleanup).
- [ ] Deletion events appear in the structured log with actor ID, note ID, and ownership relationship.
- [ ] Submitting the delete form without a valid CSRF token is rejected.

---

### Feature REQ-008: Note Sharing

**Original**: Note owners generate unique share links; anyone with the link can view the note without authentication.

**Augmented Requirements**:
- Share tokens must be cryptographically random with ΓëÑ128 bits of entropy; UUIDs (v4) are a minimum acceptable baseline (ASVS 3.2.4).
- Share link views must not expose any PII beyond the note content itself ΓÇö no owner email, no owner's private profile data.
- Share links must be scoped: each token grants access to exactly one note's public view.
- The unauthenticated share-link view endpoint must be rate-limited to prevent brute-force token enumeration.
- Revocation must invalidate the token server-side immediately; subsequent requests with a revoked token must return HTTP 404 or 410.
- A note made private while a share link exists must still be inaccessible via the share link (share-link access follows note visibility policy, or share links must be explicitly revoked on visibility downgrade).
- Share link creation, access, and revocation must all be logged.

**ASVS Mapping**: V3.2.4, V4.1.1, V8.1.1, V7.2.1, V11.1.1

**SSEM Implementation Notes**:
- **Analyzability**: Share token lifecycle (create, validate, revoke) must be in a dedicated `ShareLinkService`; the share-view controller must call only validation methods, not token generation logic.
- **Modifiability**: Share link expiry (currently unlimited) should be an optional configurable field to allow future time-bounded sharing without redesign.
- **Testability**: Test cases must cover: revoked token returns 404; private-note share link is blocked; token entropy is verified programmatically.
- **Confidentiality**: Share link pages must render note content only; they must not render the owner's email or other profile details. `Cache-Control: no-store` for private notes shared via link.
- **Accountability**: Log share link creation, each access (token, IP, timestamp), and revocation events.
- **Authenticity**: Token must be validated against the database on every request ΓÇö there is no client-side trust for share access.
- **Availability**: Rate limit the share-link view endpoint: e.g., 60 requests/minute/IP to prevent enumeration without blocking legitimate sharing.
- **Integrity**: Visibility downgrade (publicΓåÆprivate) must cascade to revoke or suspend all active share links for that note, or enforce note visibility on share-link access.
- **Resilience**: If share token lookup fails (DB timeout), fail closed ΓÇö return an error, not access to the note.

**FIASSE Tenet Annotations**:
- S2.1: Share links with no expiry are a static assumption ΓÇö optional expiry configuration allows policy to evolve without redesign.
- S2.2: Rate limiting on the unauthenticated share endpoint preserves availability while blocking enumeration.
- S2.3: High-entropy tokens and rate limiting together make token enumeration computationally impractical.
- S2.4: Server-side token validation on every request is a systematic control ΓÇö no client-side caching of authorization state.
- S2.6: Share access events in structured logs enable detection of excessive access from suspicious IPs (indicator of token leakage).

**Updated Acceptance Criteria**:
- [ ] Share tokens are UUIDs v4 or equivalent (ΓëÑ128 bits entropy); sequential or short tokens are not generated.
- [ ] Revoking a share link causes subsequent requests with that token to return HTTP 404.
- [ ] Accessing a share link for a private note returns HTTP 404 (share link does not reveal note existence).
- [ ] Share link page does not expose the owner's email address or other profile data.
- [ ] More than 60 requests/minute to the share-view endpoint from a single IP results in HTTP 429.
- [ ] Share link creation, access, and revocation events appear in the structured log.

---

### Feature REQ-009: Public/Private Notes

**Original**: Notes have a visibility toggle; public notes are searchable and viewable by anyone; private notes visible only to owner.

**Augmented Requirements**:
- Visibility enforcement must occur server-side on every request ΓÇö a note's visibility must be re-checked at the time of access, not assumed from session state or client parameter.
- Visibility changes must be audit-logged: previous state, new state, actor, timestamp.
- Search and listing queries must enforce visibility filters at the database query level, not through post-query filtering (to prevent information leakage through timing or pagination).
- Direct-object access to a private note by a non-owner must return HTTP 404 (not 403) to avoid confirming note existence.

**ASVS Mapping**: V4.1.1, V4.1.2, V4.1.3, V4.3.1

**SSEM Implementation Notes**:
- **Analyzability**: Visibility enforcement must be a shared utility method or query filter applied consistently across all note-retrieval code paths.
- **Modifiability**: Future visibility levels (e.g., "shared with specific users") must be accommodatable without restructuring the visibility model.
- **Testability**: Automated test must confirm: authenticated non-owner cannot access private note; non-authenticated user cannot access private note; both return 404.
- **Confidentiality**: Private note titles and content must not appear in any response to unauthorized actors ΓÇö including error messages.
- **Accountability**: Visibility change events logged with old value, new value, actor, and timestamp.
- **Authenticity**: Visibility checks must use the server-side authenticated identity ΓÇö client-supplied visibility bypass parameters must be ignored.
- **Availability**: Database-level visibility filtering must use indexed columns to prevent full-table scans on large note sets.
- **Integrity**: Visibility state must be a server-controlled property ΓÇö clients must not be able to force visibility state via form tampering.
- **Resilience**: A failure to load visibility state (e.g., DB read error) must fail closed ΓÇö deny access, do not default to public.

**FIASSE Tenet Annotations**:
- S2.1: The visibility model should be designed as extensible (enum/table) rather than a boolean ΓÇö future sharing levels are foreseeable.
- S2.2: Database-indexed visibility filtering ensures note access remains performant as data volume scales.
- S2.3: Returning 404 (not 403) on private note access prevents an attacker from enumerating which note IDs exist as private.
- S2.4: Server-side query-level enforcement is a systematic control ΓÇö it cannot be bypassed by UI manipulation or direct HTTP requests.
- S2.6: Visibility change audit events support data-governance investigations and anomaly detection.

**Updated Acceptance Criteria**:
- [ ] Direct GET request to a private note by a non-owner (authenticated or not) returns HTTP 404.
- [ ] Private note titles do not appear in search results for non-owners.
- [ ] Changing note visibility generates an audit log entry with before/after state.
- [ ] Visibility enforcement is applied in the database query (not post-query filtering), verifiable by query inspection.

---

### Feature REQ-010: Note Rating

**Original**: Authenticated users rate notes 1ΓÇô5 stars with optional comment; users can edit their own rating.

**Augmented Requirements**:
- Rating values must be validated server-side: integer in range 1ΓÇô5 (no other values accepted).
- One rating per user per note must be enforced server-side via a unique constraint and service-level check.
- A user must not be able to rate their own note (self-rating prevention).
- Rating creation and editing must require a valid CSRF token.
- Ratings on private notes must only be viewable by the note owner (not publicly exposed via API).
- Rating comment must be sanitized server-side; max length enforced (e.g., 1,000 characters).

**ASVS Mapping**: V4.2.1, V5.1.1, V5.1.3, V11.1.1, V11.1.5

**SSEM Implementation Notes**:
- **Analyzability**: Rating validation logic must be in a `RatingService`, not in the view or controller.
- **Modifiability**: Rating scale range (1ΓÇô5) should be configurable to allow future scale changes.
- **Testability**: Test must cover: self-rating rejection; out-of-range value rejection; duplicate rating rejection; comment length enforcement.
- **Confidentiality**: Rating comments (user-generated) must be output-encoded on display; they must not expose other users' data.
- **Accountability**: Log rating creation and edits: `{ event: "note.rated|rating.edited", noteId, ratingUserId, rating, timestamp }`.
- **Authenticity**: The rating must be bound to the server-session user ID ΓÇö client-supplied `userId` in the rating payload must be ignored.
- **Availability**: Rating endpoint must be rate-limited per user to prevent rating-spam abuse.
- **Integrity**: Unique constraint in the database must back up the service-level one-rating-per-user check; neither alone is sufficient.
- **Resilience**: Duplicate rating submission must return a meaningful error (409 Conflict), not silently fail or allow duplication.

**FIASSE Tenet Annotations**:
- S2.1: Server-side value validation prevents rating manipulation that would otherwise undermine the rating system's value.
- S2.2: Rate limiting on rating submission preserves the integrity of the rating system under bot-driven manipulation attempts.
- S2.3: Self-rating prevention and one-rating-per-user constraints reduce the probability of gamified rating inflation.
- S2.4: Database unique constraint + service check creates defense-in-depth ΓÇö neither layer alone is relied upon.
- S2.6: Structured rating events enable analysis of suspicious voting patterns and provide an audit trail for disputes.

**Updated Acceptance Criteria**:
- [ ] Submitting a rating of 0 or 6 is rejected with a validation error.
- [ ] A user attempting to rate their own note receives a rejection response.
- [ ] A second rating submission by the same user for the same note returns HTTP 409.
- [ ] Rating comment longer than 1,000 characters is rejected with a validation error.
- [ ] Rating event appears in structured log with note ID, rater user ID, and rating value.

---

### Feature REQ-011: Rating Management

**Original**: Note owners can view all ratings on their notes including value, comment, rater username, and average; sorted by date.

**Augmented Requirements**:
- Access to the full rating list for a note must be restricted to the note owner (or admin) ΓÇö ratings must not be enumerable by arbitrary users.
- Rater username must be displayed without exposing rater email or other PII.
- The average rating calculation must occur server-side; do not trust client-supplied aggregation parameters.
- Pagination must be enforced on the rating list to prevent unbounded data responses.

**ASVS Mapping**: V4.1.3, V8.1.1, V8.3.1

**SSEM Implementation Notes**:
- **Analyzability**: Rating aggregation (average calculation) should be a dedicated repository/service method, not inline SQL in the controller.
- **Modifiability**: Sorting order (newest first) should be configurable or accept a sort parameter validated against an allowlist.
- **Testability**: Test must confirm non-owners receive 403 on the rating management endpoint.
- **Confidentiality**: Rater email addresses must not be included in the rating list response, even for note owners.
- **Accountability**: Admin access to rating lists must be logged separately from owner access.
- **Authenticity**: The note ID in the request must be verified against the authenticated session owner before ratings are returned.
- **Availability**: Paginate rating list (e.g., 25 per page) to prevent large responses causing performance issues.
- **Integrity**: Average rating is computed from database-stored values ΓÇö not from any client input.
- **Resilience**: Rating list endpoint must handle notes with zero ratings gracefully (return empty list, not error).

**FIASSE Tenet Annotations**:
- S2.1: Pagination and server-side aggregation ensure the feature remains functional as note popularity grows.
- S2.2: Bounded page sizes protect server performance under high-rating-volume scenarios.
- S2.3: Restricting rater PII to username only minimizes exposure in the event of a rating list data leak.
- S2.4: Server-side average calculation prevents gamification via client-side parameter manipulation.
- S2.6: Owner-access events to rating lists are auditable for privacy investigations.

**Updated Acceptance Criteria**:
- [ ] Non-owner authenticated user requesting the rating management view for another user's note receives HTTP 403.
- [ ] Rater email addresses are not included in any rating list response.
- [ ] Rating list is paginated; requesting beyond the last page returns an empty result, not an error.
- [ ] Average rating is computed server-side and matches the stored rating values.

---

### Feature REQ-012: Note Search

**Original**: Users search notes by keywords; results include owned notes (any visibility) and public notes from others; excludes other users' private notes.

**Augmented Requirements**:
- Search query must be validated server-side: non-empty, maximum length enforced (e.g., 500 characters) to prevent DoS.
- Search query must be parameterized at the database layer ΓÇö no dynamic SQL string interpolation.
- Output in search results (titles, excerpts) must be HTML-output-encoded before rendering.
- Visibility filter must be applied at query level (database predicate), not post-query.
- Search results must be paginated to bound response size and prevent information over-exposure.
- Search endpoint must be rate-limited per authenticated session to prevent search-based enumeration.

**ASVS Mapping**: V5.1.1, V5.1.3, V5.3.1, V5.3.4, V4.1.1, V4.1.2

**SSEM Implementation Notes**:
- **Analyzability**: Search logic must be in a named `NoteSearchService`; visibility filtering and query parameterization must be identifiable as distinct operations in code review.
- **Modifiability**: Search relevance ranking and field selection should be centralized to allow future upgrade to full-text search engines.
- **Testability**: Integration tests must confirm: private notes from other users are not returned; SQL injection payloads return no results or validation error; XSS payloads in note titles are escaped in output.
- **Confidentiality**: Search results must not expose note content beyond the defined excerpt (first 200 characters); full content must require explicit note view.
- **Accountability**: Search queries may be logged at debug level (without user-identifying data) for performance analysis; do not log search terms in a way that creates a surveillance record of user interests.
- **Authenticity**: Search visibility filtering uses the server-side authenticated identity ΓÇö unauthenticated users see only public notes.
- **Availability**: Enforce query length limits and rate limiting; consider query complexity limits for future full-text search implementations.
- **Integrity**: Visibility filtering is a WHERE clause predicate, not a post-fetch filter ΓÇö results are correct by construction.
- **Resilience**: Search service must return an empty result set (not an error) when no matches are found; it must handle malformed queries with a validation error, not a 500.

**FIASSE Tenet Annotations**:
- S2.1: Parameterized queries protect against SQL injection without requiring input sanitization ΓÇö a more robust, adaptable control.
- S2.2: Pagination and rate limiting maintain search availability under high load.
- S2.3: Query-level visibility enforcement prevents accidental data exposure due to post-query filter bugs.
- S2.4: Output encoding in search results is a systematic control ΓÇö it applies to all result rendering, not per-field patches.
- S2.6: Logging search activity (bounded, anonymized) enables detection of content enumeration patterns.

**Updated Acceptance Criteria**:
- [ ] Search query longer than 500 characters is rejected with a validation error.
- [ ] Search results for an authenticated user never include private notes from other users.
- [ ] Search results containing note titles with HTML special characters render them escaped (e.g., `<script>` appears as literal text).
- [ ] More than 30 search requests/minute from a single session returns HTTP 429.
- [ ] Search results are paginated (e.g., 20 per page).

---

### Feature REQ-013: Admin Dashboard

**Original**: Admin users view total counts, recent activity, user list, and can search users.

**Augmented Requirements**:
- All admin endpoints must be protected by both authentication and role authorization ([Authorize(Roles = "Admin")]) verified server-side on every request (ASVS 4.3.1).
- Admin sessions must have a reduced idle timeout (e.g., 15 minutes) compared to regular user sessions.
- Destructive or high-impact admin actions (user disabling, note deletion) must require re-authentication or explicit confirmation token.
- User search in admin context must be parameterized and paginated; results must not expose password hashes or tokens.
- Activity log display must be read-only; admins must not be able to delete or modify audit log records through the UI.
- Admin access to the dashboard must itself be audit-logged.

**ASVS Mapping**: V4.3.1, V4.3.2, V4.3.3, V7.2.1, V2.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Admin controller and regular user controller must be separate classes; do not mix authorization levels in a single controller.
- **Modifiability**: Role checking should use policy-based authorization (ASP.NET Core authorization policies) rather than inline role string comparisons, to allow future role additions.
- **Testability**: Integration tests must confirm non-admin users receive HTTP 403 on all admin endpoints, including direct URL access.
- **Confidentiality**: Admin user list must not expose password hashes, reset tokens, or security stamps; select only display-safe fields.
- **Accountability**: Every admin dashboard page load and action must produce a structured audit log entry: `{ event: "admin.access|admin.action", adminId, targetUserId/noteId, action, timestamp }`.
- **Authenticity**: Admin role must be verified from the server-side claims, not from any client-submitted header or cookie value.
- **Availability**: Paginate user lists; admin dashboard aggregate counts should be computed asynchronously to prevent blocking requests.
- **Integrity**: Activity log records must be append-only; no admin UI path should allow log modification.
- **Resilience**: Admin dashboard must degrade gracefully if the activity log service is unavailable ΓÇö show an error message, do not expose a blank page or raw exception.

**FIASSE Tenet Annotations**:
- S2.1: Policy-based authorization allows role models to evolve without code changes per endpoint.
- S2.2: Asynchronous dashboard data loading and pagination maintain availability under high data volume.
- S2.3: Append-only audit logs with no UI delete path protect the integrity of the forensic record.
- S2.4: Server-side role claim verification is the systematic control; UI hiding of admin links is cosmetic only.
- S2.6: Admin access events in structured logs are the primary detection mechanism for unauthorized privilege escalation.

**Updated Acceptance Criteria**:
- [ ] Authenticated non-admin user navigating directly to `/admin/*` routes receives HTTP 403.
- [ ] Admin dashboard page load produces an audit log entry with admin user ID and timestamp.
- [ ] User list in admin view does not expose password hash, security stamp, or reset token fields.
- [ ] Activity log records cannot be deleted via any admin UI action.
- [ ] Admin session expires after 15 minutes of inactivity.

---

### Feature REQ-014: User Profile Management

**Original**: Users update username, email, and password; changes saved to database.

**Augmented Requirements**:
- Password change must require confirmation of the current password before accepting a new one (ASVS 2.1.4).
- New password must comply with the same policy as registration (ΓëÑ12 chars, breach corpus check).
- Email address change must trigger a notification email sent to the *previous* email address (ASVS 2.5.6).
- Username change must enforce same uniqueness and length constraints as registration.
- All profile update fields must be validated server-side; CSRF anti-forgery token required.
- Profile update form must not allow a user to change their own role (privilege escalation via profile edit).

**ASVS Mapping**: V2.1.1, V2.1.7, V2.5.6, V4.1.3, V5.1.1, V5.1.3

**SSEM Implementation Notes**:
- **Analyzability**: Profile update operations must be decomposed: password change, email change, and username change are distinct operations with distinct validation paths.
- **Modifiability**: Notification triggers (email-change notification) should be event-driven (domain event or service hook) so new notification types can be added without modifying the profile controller.
- **Testability**: Test must confirm: current-password verification fails on wrong password; role field in profile form is ignored or rejected.
- **Confidentiality**: Profile update response must not echo back the new password or any token values.
- **Accountability**: Log all profile changes: `{ event: "profile.updated", userId, fieldsChanged, timestamp }`.
- **Authenticity**: Profile update must operate on the authenticated user's own ID ΓÇö users must not be able to update another user's profile by ID manipulation.
- **Availability**: Profile update endpoint must be rate-limited to prevent brute-force credential confirmation.
- **Integrity**: Email format must be validated and normalized (lowercase, trimmed) on update with the same canonicalization as registration.
- **Resilience**: Partial profile update failure (e.g., email save succeeds but notification email fails) must be handled: notification failure must be logged but must not roll back the user's saved email change.

**FIASSE Tenet Annotations**:
- S2.1: Event-driven notification architecture allows new notification policies (e.g., SMS on email change) without redesigning the profile update flow.
- S2.2: Rate limiting on profile updates prevents brute-force current-password confirmation attacks.
- S2.3: Current-password confirmation before change, and notification to old email on email change, together reduce account-takeover probability.
- S2.4: Server-side role field exclusion is a systematic control ΓÇö even if a malicious client injects a role field, it is ignored.
- S2.6: Profile change events in structured logs are the primary record for account-compromise investigations.

**Updated Acceptance Criteria**:
- [ ] Attempting a password change without supplying the correct current password is rejected.
- [ ] New password violating the complexity policy (length, breach corpus) is rejected at profile update.
- [ ] Changing email address sends a notification to the previous email address.
- [ ] Injecting a `role=Admin` field into the profile update form does not change the user's role.
- [ ] Profile update event is emitted to the structured log with the fields changed (not the new values for password).

---

### Feature REQ-015: Top Rated Notes

**Original**: Public notes sorted by average rating (descending); requires minimum 3 ratings; shows title, author, average, count, preview.

**Augmented Requirements**:
- Only public notes must appear in the top-rated list ΓÇö visibility enforcement must be at the query level.
- Minimum distinct-rater threshold (not just minimum rating count) should be considered to prevent a single user inflating a note's rating to the top.
- Rating aggregation must be computed server-side; pagination must be enforced.
- Preview text (excerpt) must be output-encoded before rendering.
- This endpoint is accessible to unauthenticated users (public page); rate limiting must be applied to prevent scraping.

**ASVS Mapping**: V4.1.1, V5.3.1, V11.1.2

**SSEM Implementation Notes**:
- **Analyzability**: Top-rated query logic must be in a named service method; sorting and filtering criteria must be readable and documented.
- **Modifiability**: The minimum-rating threshold (3) must be configurable, not hardcoded.
- **Testability**: Test must confirm: private notes never appear in top-rated; a note with ratings from one user (count > 3) does not appear if distinct-rater rule is applied.
- **Confidentiality**: Top-rated list must not expose author email ΓÇö only display name/username.
- **Accountability**: Access to top-rated page may be logged at aggregate level for analytics (page views, not individual user tracking).
- **Authenticity**: Pagination cursor/offset parameters must be validated as non-negative integers; malformed pagination values must return a validation error.
- **Availability**: Top-rated query must be indexed and potentially cached (with short TTL) to handle high public traffic; avoid unbounded aggregation on large datasets.
- **Integrity**: Average rating in top-rated list must be computed from stored ratings ΓÇö not from any cached or client-supplied value.
- **Resilience**: If the rating aggregation query is slow or times out, return a service-unavailable response with retry guidance, not a raw exception.

**FIASSE Tenet Annotations**:
- S2.1: Configurable thresholds allow the anti-gaming policy to evolve as manipulation patterns are observed.
- S2.2: Query indexing and short-TTL caching protect availability of the public-facing top-rated page under traffic spikes.
- S2.3: Distinct-rater constraints reduce the probability that a coordinated rating campaign successfully manipulates top-rated rankings.
- S2.4: Server-side enforcement of visibility and aggregation thresholds is the systematic control ΓÇö UI hiding of private notes is insufficient.
- S2.6: Aggregate access logging (page views, not individual user tracking) enables performance monitoring without creating a surveillance record.

**Updated Acceptance Criteria**:
- [ ] A private note with 10 five-star ratings does not appear in the top-rated list.
- [ ] A note with 3 or more ratings but all from the same user does not appear if distinct-rater rule is configured.
- [ ] Top-rated list is paginated (e.g., 20 per page).
- [ ] Preview text containing HTML special characters is displayed escaped.
- [ ] More than 120 requests/minute from a single IP to the top-rated endpoint returns HTTP 429.

---

### Feature REQ-016: Note Ownership Reassignment

**Original**: Admin can change the owner of any note to a different existing user.

**Augmented Requirements**:
- Operation is restricted to admin role only; server-side role assertion required on every request.
- Target user must exist and be verified from the database before reassignment is persisted.
- Reassignment must produce a structured audit log entry with: previous owner ID, new owner ID, note ID, acting admin ID, and timestamp.
- Admin must be prevented from reassigning a note to a non-existent user ID via direct parameter manipulation.
- CSRF anti-forgery token required on the reassignment form.
- After reassignment, both previous and new owners should receive notification (optional, but recommended for accountability).

**ASVS Mapping**: V4.3.1, V4.1.3, V7.2.1

**SSEM Implementation Notes**:
- **Analyzability**: Ownership transfer must be a dedicated, named service method (`NoteService.TransferOwnership(noteId, newOwnerId, actingAdminId)`) ΓÇö not an inline controller update.
- **Modifiability**: Notification dispatch on reassignment should be event-driven to allow policy changes without modifying the transfer logic.
- **Testability**: Integration test must confirm: non-admin receives 403; reassigning to a non-existent user ID returns a validation error; audit log entry is created.
- **Confidentiality**: The reassignment endpoint must not expose the note content in the response ΓÇö only confirmation of success.
- **Accountability**: Audit log entry is mandatory and must be written atomically with the ownership update (in the same database transaction if possible).
- **Authenticity**: Both the note ID and the target user ID must be validated against the database ΓÇö no parameter must be trusted without server-side verification.
- **Availability**: Reassignment is an infrequent operation; no special availability concern, but it must complete in a bounded time.
- **Integrity**: Note ownership update and audit log entry must be in the same database transaction; if either fails, neither is committed.
- **Resilience**: Failed reassignment (target user not found, database error) must return a specific error message to the admin without exposing internal stack traces.

**FIASSE Tenet Annotations**:
- S2.1: Transactional audit logging ensures the accountability record survives even as the codebase evolves.
- S2.2: Bounded validation at the service layer ensures the feature degrades gracefully under malformed inputs.
- S2.3: Mandatory audit log reduces the material impact of unauthorized reassignment by ensuring it is detectable.
- S2.4: Server-side existence check for target user is a systematic control ΓÇö it prevents object injection through parameter guessing.
- S2.6: Atomic audit log with the ownership update is the core transparency guarantee for this feature.

**Updated Acceptance Criteria**:
- [ ] Non-admin authenticated user POSTing to the reassignment endpoint receives HTTP 403.
- [ ] Reassigning a note to a user ID that does not exist returns a validation error (not 500).
- [ ] After successful reassignment, a structured audit log entry exists with previous owner, new owner, note ID, admin ID, and timestamp.
- [ ] Audit log entry and ownership update are either both persisted or both rolled back.
- [ ] CSRF token validation failure on the reassignment form returns HTTP 400/403.

---

## Global Securability Requirements

These cross-cutting requirements apply to the entire application and are not specific to a single feature.

### G-001: Transport Security
- All HTTP requests must be redirected to HTTPS; HSTS header must be set with `max-age ΓëÑ 31536000` (ASVS 9.1.1ΓÇô9.1.2).
- TLS 1.2 is the minimum; TLS 1.3 is preferred; SSLv3, TLS 1.0, TLS 1.1 must be disabled.

### G-002: Security Response Headers
- All responses must include: `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY` (or `SAMEORIGIN`), `Referrer-Policy: strict-origin-when-cross-origin` (ASVS 14.4.1ΓÇô14.4.6).
- CSP must restrict script sources to known origins; `unsafe-inline` and `unsafe-eval` must be avoided.

### G-003: Error Handling
- All unhandled exceptions must be caught by a global error handler; stack traces must not be exposed to end users (ASVS 7.4.1).
- Custom error pages must be used for 404, 403, 500 status codes.
- Errors must be logged with full context server-side; clients receive only a correlation ID for support.

### G-004: Dependency Management
- All NuGet dependencies must be pinned to specific versions; a dependency vulnerability scan (e.g., `dotnet list package --vulnerable`) must be incorporated into the CI pipeline (ASVS 14.2.1).
- Dependencies with known critical CVEs must be updated before deployment.

### G-005: Structured Logging Schema
- All application log events must follow a consistent schema: `{ timestamp, level, event, actorId, targetId, outcome, correlationId, ip }`.
- Sensitive fields (passwords, tokens, PII beyond user ID) must never appear in log output.
- Log output must be directed to a structured sink (e.g., Serilog with JSON output) suitable for SIEM ingestion.

### G-006: Database Security
- The application database account must follow least-privilege: SELECT, INSERT, UPDATE, DELETE on application tables only ΓÇö no DDL privileges at runtime.
- Connection strings must be stored in environment variables or a secrets manager ΓÇö never in committed `appsettings.json` files.

### G-007: Secrets Management
- JWT signing keys, API keys, and database credentials must not be hardcoded or committed to source control.
- Use environment variables, Azure Key Vault, or AWS Secrets Manager for production secrets.

### G-008: Anti-CSRF Policy
- All state-changing requests (POST, PUT, PATCH, DELETE) must include and validate an anti-forgery token using ASP.NET Core's built-in `[ValidateAntiForgeryToken]` attribute or equivalent global filter.

---

## Open Gaps and Assumptions

| ID | Gap / Assumption | Risk | Recommended Action |
|----|-----------------|------|-------------------|
| GAP-001 | Email delivery mechanism is not specified (SMTP, SendGrid, etc.) | Password reset and email-change notification reliability is unproven | Define email service provider and failure behavior before implementation |
| GAP-002 | File storage backend (local vs. cloud) is not finalized | Path traversal and access control implementation differ significantly between backends | Decide storage backend before implementing `FileStorageService`; define the interface contract now |
| GAP-003 | No multi-factor authentication (MFA) is specified | Accounts are vulnerable to credential stuffing with no second factor | Consider adding TOTP/email-OTP MFA as a Level 2+ enhancement; ASVS 2.2.1 recommends MFA for all Level 2 systems |
| GAP-004 | No soft-delete or recovery mechanism is defined for notes | Accidental or malicious deletion has no recourse | Define retention/recovery policy before implementation |
| GAP-005 | Share links have no expiry by default | Indefinitely valid share links increase exposure surface over time | Add optional expiry field to share links in the data model; enforce configurable default maximum lifetime |
| GAP-006 | No account deactivation / suspension mechanism is defined | Admin cannot disable a compromised account without deletion | Add account active/inactive state; deactivation must prevent login without deleting data |
| GAP-007 | Breach-corpus password check API dependency is not specified | Unavailability of external API may block registration/password change | Define fallback behavior (warn but allow, or block); document SLA dependency |
| GAP-008 | Rate limiting infrastructure is not specified | Individual endpoint rate limits require a shared counter store (Redis, in-memory, etc.) | Select rate-limiting backend before implementation; in-memory limits are not effective across multiple application instances |
| GAP-009 | Admin activity log retention policy is not defined | Logs may be overwritten or lost; compliance investigations lack sufficient history | Define minimum log retention period (recommend 90 days online, 1 year archive) |
| GAP-010 | No content moderation or abuse reporting mechanism is specified | Public notes and ratings can contain offensive or harmful content | Define policy for content moderation; at minimum, add a report mechanism to the backlog |

---

*Enhanced with FIASSE/SSEM securability engineering principles and OWASP ASVS Level 2 requirement mapping. Generated 2026-03-26.*
