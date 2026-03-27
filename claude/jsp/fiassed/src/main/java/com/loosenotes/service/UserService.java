package com.loosenotes.service;

import com.loosenotes.dao.UserDao;
import com.loosenotes.model.AuditEvent;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Application-layer service for all user-account operations.
 *
 * <p><strong>Design principles applied:</strong>
 * <ul>
 *   <li><em>Authenticity / Accountability</em> — every sensitive operation
 *       (registration, login, lockout, password change) emits an audit event
 *       with the subject IP and outcome.</li>
 *   <li><em>Confidentiality</em> — {@link #authenticate} returns the same
 *       {@code null} result whether credentials are wrong OR the account is
 *       locked, preventing username-enumeration and lockout-state disclosure.</li>
 *   <li><em>Modifiability</em> — lockout thresholds are named constants, not
 *       magic numbers scattered across methods.</li>
 * </ul>
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    // --- Lockout policy constants (single place to change thresholds) ---

    /** Number of consecutive failed logins before an account is locked. */
    public static final int MAX_ATTEMPTS = 5;

    /** Duration of an automatic lockout in minutes. */
    public static final int LOCKOUT_MINUTES = 15;

    // --- Collaborators ---

    private final UserDao userDao;
    private final PasswordPolicyService passwordPolicyService;
    private final AuditService auditService;

    /**
     * All collaborators are required (fail-fast on null).
     */
    public UserService(UserDao userDao,
                       PasswordPolicyService passwordPolicyService,
                       AuditService auditService) {
        if (userDao == null) throw new IllegalArgumentException("userDao must not be null");
        if (passwordPolicyService == null) throw new IllegalArgumentException("passwordPolicyService must not be null");
        if (auditService == null) throw new IllegalArgumentException("auditService must not be null");

        this.userDao = userDao;
        this.passwordPolicyService = passwordPolicyService;
        this.auditService = auditService;
    }

    // =========================================================================
    // Registration
    // =========================================================================

    /**
     * Registers a new user account.
     *
     * <p>Validates inputs, hashes the password (plain-text never persisted),
     * creates the record and emits an audit event.
     *
     * @param username  desired username
     * @param email     email address
     * @param password  plain-text password (discarded after hashing)
     * @param ipAddress caller IP for audit trail
     * @return the newly-created {@link User}
     * @throws ServiceException with code "VALIDATION" on policy or format
     *                          violations, "DUPLICATE" if username/email is taken
     */
    public User register(String username, String email, String password, String ipAddress)
            throws ServiceException {

        // --- Input validation ---
        if (!ValidationUtil.isValidUsername(username)) {
            throw new ServiceException("VALIDATION",
                    "Username must be 3-50 characters and contain only letters, digits, underscores, or hyphens.");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ServiceException("VALIDATION", "Email address is not valid.");
        }

        PasswordPolicyService.ValidationResult pwResult = passwordPolicyService.validate(password);
        if (!pwResult.isValid()) {
            throw new ServiceException("VALIDATION", pwResult.getMessage());
        }

        // --- Uniqueness checks ---
        if (userDao.findByUsername(username) != null) {
            throw new ServiceException("DUPLICATE", "Username is already taken.");
        }
        if (userDao.findByEmail(email) != null) {
            throw new ServiceException("DUPLICATE", "Email address is already registered.");
        }

        // --- Create record ---
        String passwordHash = PasswordUtil.hash(password);
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(User.Role.USER);
        user.setFailedLoginAttempts(0);
        Instant now = Instant.now();
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        User created = userDao.create(user);

        // --- Audit ---
        auditService.recordEvent(AuditEvent.builder()
                .action("USER_REGISTERED")
                .subjectId(String.valueOf(created.getId()))
                .subjectUsername(created.getUsername())
                .ipAddress(ipAddress)
                .outcome("SUCCESS")
                .build());

        log.info("User registered. userId={} username={} ip={}", created.getId(), created.getUsername(), ipAddress);
        return created;
    }

    // =========================================================================
    // Authentication
    // =========================================================================

    /**
     * Authenticates a user by username and password.
     *
     * <p><strong>Confidentiality</strong>: the method returns {@code null} for
     * BOTH wrong-password and locked-account scenarios.  A uniform "invalid
     * credentials" response is sent to callers so that neither lockout state
     * nor account existence can be inferred from the response alone.
     *
     * @param username  supplied username
     * @param password  supplied plain-text password
     * @param ipAddress caller IP for audit trail
     * @return authenticated {@link User}, or {@code null} on any failure
     */
    public User authenticate(String username, String password, String ipAddress) {

        User user = userDao.findByUsername(username);

        if (user == null) {
            // Do NOT reveal non-existence; log for ops only at DEBUG.
            log.debug("Authentication attempt for unknown username. ip={}", ipAddress);
            emitAuthFailureAudit(null, username, ipAddress, "UNKNOWN_USER");
            return null;
        }

        // --- Lockout check FIRST (must precede password verification) ---
        if (isLockedOut(user)) {
            log.info("Authentication rejected; account locked. userId={} ip={}", user.getId(), ipAddress);
            emitAuthFailureAudit(user, username, ipAddress, "ACCOUNT_LOCKED");
            // Return null — same as wrong password; do not tell caller why.
            return null;
        }

        // --- Password verification ---
        boolean passwordMatches = PasswordUtil.verify(password, user.getPasswordHash());

        if (!passwordMatches) {
            handleFailedAttempt(user, ipAddress);
            return null;
        }

        // --- Success path ---
        resetFailedAttempts(user);

        auditService.recordEvent(AuditEvent.builder()
                .action("USER_LOGIN")
                .subjectId(String.valueOf(user.getId()))
                .subjectUsername(user.getUsername())
                .ipAddress(ipAddress)
                .outcome("SUCCESS")
                .build());

        log.info("User authenticated. userId={} ip={}", user.getId(), ipAddress);
        return user;
    }

    // =========================================================================
    // Account lockout
    // =========================================================================

    /**
     * Administratively locks a user account for a given duration.
     *
     * @param userId          target user ID
     * @param durationMinutes lock duration (positive integer)
     */
    public void lockAccount(Long userId, int durationMinutes) {
        if (durationMinutes <= 0) {
            throw new IllegalArgumentException("durationMinutes must be positive");
        }
        Instant lockoutUntil = Instant.now().plus(durationMinutes, ChronoUnit.MINUTES);
        userDao.updateLockout(userId, lockoutUntil);

        auditService.recordEvent(AuditEvent.builder()
                .action("ACCOUNT_LOCKED")
                .objectId(String.valueOf(userId))
                .metadata("durationMinutes=" + durationMinutes)
                .outcome("SUCCESS")
                .build());

        log.info("Account locked by admin. userId={} durationMinutes={}", userId, durationMinutes);
    }

    // =========================================================================
    // Profile mutation (all require current-password re-verification)
    // =========================================================================

    /**
     * Changes a user's password after verifying the current one.
     *
     * @param userId          the user performing the change
     * @param currentPassword plain-text current password for re-verification
     * @param newPassword     plain-text new password
     * @throws ServiceException VALIDATION on policy failure, ACCESS_DENIED on
     *                          wrong current password, NOT_FOUND if user missing
     */
    public void changePassword(Long userId, String currentPassword, String newPassword)
            throws ServiceException {

        User user = requireUser(userId);
        verifyCurrentPassword(user, currentPassword);

        PasswordPolicyService.ValidationResult result = passwordPolicyService.validate(newPassword);
        if (!result.isValid()) {
            throw new ServiceException("VALIDATION", result.getMessage());
        }

        String newHash = PasswordUtil.hash(newPassword);
        userDao.updatePasswordHash(userId, newHash);

        auditService.recordEvent(AuditEvent.builder()
                .action("PASSWORD_CHANGED")
                .subjectId(String.valueOf(userId))
                .subjectUsername(user.getUsername())
                .outcome("SUCCESS")
                .build());

        log.info("Password changed. userId={}", userId);
    }

    /**
     * Changes a user's username after verifying their current password.
     *
     * @param userId          the user performing the change
     * @param currentPassword plain-text current password for re-verification
     * @param newUsername     desired new username
     * @throws ServiceException on validation failure, access denial, or duplicate
     */
    public void changeUsername(Long userId, String currentPassword, String newUsername)
            throws ServiceException {

        User user = requireUser(userId);
        verifyCurrentPassword(user, currentPassword);

        if (!ValidationUtil.isValidUsername(newUsername)) {
            throw new ServiceException("VALIDATION",
                    "Username must be 3-50 characters and contain only letters, digits, underscores, or hyphens.");
        }
        if (userDao.findByUsername(newUsername) != null) {
            throw new ServiceException("DUPLICATE", "Username is already taken.");
        }

        userDao.updateUsername(userId, newUsername);

        auditService.recordEvent(AuditEvent.builder()
                .action("USERNAME_CHANGED")
                .subjectId(String.valueOf(userId))
                .subjectUsername(user.getUsername())
                .metadata("newUsername=" + newUsername)
                .outcome("SUCCESS")
                .build());

        log.info("Username changed. userId={} oldUsername={} newUsername={}", userId, user.getUsername(), newUsername);
    }

    /**
     * Changes a user's email address after verifying their current password.
     *
     * @param userId          the user performing the change
     * @param currentPassword plain-text current password for re-verification
     * @param newEmail        new email address
     * @throws ServiceException on validation failure, access denial, or duplicate
     */
    public void changeEmail(Long userId, String currentPassword, String newEmail)
            throws ServiceException {

        User user = requireUser(userId);
        verifyCurrentPassword(user, currentPassword);

        if (!ValidationUtil.isValidEmail(newEmail)) {
            throw new ServiceException("VALIDATION", "Email address is not valid.");
        }
        if (userDao.findByEmail(newEmail) != null) {
            throw new ServiceException("DUPLICATE", "Email address is already registered.");
        }

        userDao.updateEmail(userId, newEmail);

        auditService.recordEvent(AuditEvent.builder()
                .action("EMAIL_CHANGED")
                .subjectId(String.valueOf(userId))
                .subjectUsername(user.getUsername())
                .outcome("SUCCESS")
                .build());

        log.info("Email changed. userId={}", userId);
    }

    // =========================================================================
    // Lookups
    // =========================================================================

    /**
     * Finds a user by primary key.
     *
     * @return the {@link User}, or {@code null} if not found
     */
    public User findById(Long id) {
        return userDao.findById(id);
    }

    /**
     * Returns a page of users (admin use).
     *
     * @param page     0-based page index
     * @param pageSize number of records per page
     */
    public List<User> listUsers(int page, int pageSize) {
        return userDao.listAll(page, pageSize);
    }

    /**
     * Returns a page of users whose username or email matches {@code query}.
     *
     * @param query    search term
     * @param page     0-based page index
     * @param pageSize number of records per page
     */
    public List<User> searchUsers(String query, int page, int pageSize) {
        return userDao.search(query, page, pageSize);
    }

    /** Returns the total number of registered users. */
    public int countUsers() {
        return userDao.count();
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private boolean isLockedOut(User user) {
        return user.getLockoutUntil() != null
                && Instant.now().isBefore(user.getLockoutUntil());
    }

    private void handleFailedAttempt(User user, String ipAddress) {
        int attempts = user.getFailedLoginAttempts() + 1;
        Instant lockoutUntil = null;

        if (attempts >= MAX_ATTEMPTS) {
            lockoutUntil = Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES);
            userDao.updateLockout(user.getId(), lockoutUntil);
            log.warn("Account auto-locked after {} failed attempts. userId={} ip={}",
                    MAX_ATTEMPTS, user.getId(), ipAddress);
            auditService.recordEvent(AuditEvent.builder()
                    .action("ACCOUNT_AUTO_LOCKED")
                    .subjectId(String.valueOf(user.getId()))
                    .subjectUsername(user.getUsername())
                    .ipAddress(ipAddress)
                    .metadata("failedAttempts=" + attempts)
                    .outcome("SUCCESS")
                    .build());
        } else {
            userDao.incrementFailedLoginAttempts(user.getId());
        }

        emitAuthFailureAudit(user, user.getUsername(), ipAddress, "BAD_CREDENTIALS");
    }

    private void resetFailedAttempts(User user) {
        if (user.getFailedLoginAttempts() > 0 || user.getLockoutUntil() != null) {
            userDao.resetFailedLoginAttempts(user.getId());
        }
    }

    private void emitAuthFailureAudit(User user, String username, String ipAddress, String reason) {
        auditService.recordEvent(AuditEvent.builder()
                .action("USER_LOGIN_FAILED")
                .subjectId(user != null ? String.valueOf(user.getId()) : null)
                .subjectUsername(username)
                .ipAddress(ipAddress)
                .metadata("reason=" + reason)
                .outcome("FAILURE")
                .build());
    }

    /**
     * Loads a user by ID or throws {@link ServiceException} NOT_FOUND.
     */
    private User requireUser(Long userId) throws ServiceException {
        User user = userDao.findById(userId);
        if (user == null) {
            throw new ServiceException("NOT_FOUND", "User not found.");
        }
        return user;
    }

    /**
     * Verifies the supplied plain-text password against the stored hash.
     * Throws ACCESS_DENIED on mismatch — never reveals hash or algorithm details.
     */
    private void verifyCurrentPassword(User user, String currentPassword) throws ServiceException {
        if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) {
            log.warn("Current password verification failed. userId={}", user.getId());
            throw new ServiceException("ACCESS_DENIED", "Current password is incorrect.");
        }
    }
}
