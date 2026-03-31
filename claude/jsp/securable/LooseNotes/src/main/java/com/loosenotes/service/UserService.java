package com.loosenotes.service;

import com.loosenotes.dao.UserDao;
import com.loosenotes.model.AuditLog.EventType;
import com.loosenotes.model.Role;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.RateLimiter;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Business logic for user registration, authentication, and profile management.
 *
 * SSEM / ASVS alignment:
 * - ASVS V6.1 (Password): BCrypt via PasswordUtil; policy enforced here.
 * - ASVS V8.1 (Brute Force): rate limiter injected to throttle login attempts.
 * - Authenticity: login method invalidates and regenerates session (not done here –
 *   AuthServlet owns session, service owns credential logic only).
 * - Confidentiality: cleartext passwords are cleared from memory after use.
 * - Accountability: auth events delegated to AuditService.
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserDao userDao;
    private final AuditService auditService;
    private final RateLimiter loginRateLimiter;

    public UserService(UserDao userDao, AuditService auditService, RateLimiter loginRateLimiter) {
        this.userDao          = userDao;
        this.auditService     = auditService;
        this.loginRateLimiter = loginRateLimiter;
    }

    /**
     * Registers a new user.
     *
     * @param username  validated username
     * @param email     validated email
     * @param password  plaintext password (cleared after hashing)
     * @param ipAddress client IP for rate limiting
     * @return the newly created user ID
     * @throws ServiceException if validation fails or username/email is taken
     */
    public long register(String username, String email, char[] password, String ipAddress)
            throws ServiceException, SQLException {

        if (!ValidationUtil.isValidUsername(username)) {
            throw new ServiceException("Invalid username format");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ServiceException("Invalid email address");
        }
        if (!PasswordUtil.meetsPolicy(password)) {
            throw new ServiceException("Password must be 8–128 characters");
        }
        if (userDao.findByUsername(username).isPresent()) {
            throw new ServiceException("Username already taken");
        }
        if (userDao.findByEmail(email).isPresent()) {
            throw new ServiceException("Email address already registered");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(password));
        user.setRole(Role.USER);
        user.setEnabled(true);

        // Clear password from memory as soon as it is hashed
        Arrays.fill(password, '\0');

        long newId = userDao.insert(user);
        auditService.record(newId, EventType.AUTH,
                "user_registered username=" + username, ipAddress);
        log.info("User registered: username={}", username);
        return newId;
    }

    /**
     * Authenticates a user. Rate-limits by IP address.
     *
     * @param username  submitted username
     * @param password  submitted plaintext password (cleared after check)
     * @param ipAddress client IP
     * @return the authenticated User
     * @throws ServiceException if credentials are invalid or account is locked/disabled
     */
    public User authenticate(String username, char[] password, String ipAddress)
            throws ServiceException, SQLException {

        // Trust boundary: check rate limit BEFORE any database query
        if (!loginRateLimiter.tryAcquire(ipAddress)) {
            auditService.record(null, EventType.AUTH,
                    "login_rate_limited ip=" + ipAddress, ipAddress);
            throw new ServiceException("Too many login attempts. Please wait and try again.");
        }

        Optional<User> found = userDao.findByUsername(username);
        if (found.isEmpty()) {
            // Use same timing as a successful path to avoid user enumeration
            PasswordUtil.verify(password, "$2a$12$invalid_hash_to_burn_time_00000000");
            Arrays.fill(password, '\0');
            auditService.record(null, EventType.AUTH,
                    "login_failed username=" + username, ipAddress);
            throw new ServiceException("Invalid username or password");
        }

        User user = found.get();
        boolean valid = PasswordUtil.verify(password, user.getPasswordHash());
        Arrays.fill(password, '\0');

        if (!valid) {
            auditService.record(user.getId(), EventType.AUTH,
                    "login_failed username=" + username, ipAddress);
            throw new ServiceException("Invalid username or password");
        }
        if (!user.isEnabled()) {
            auditService.record(user.getId(), EventType.AUTH,
                    "login_disabled username=" + username, ipAddress);
            throw new ServiceException("Account is disabled. Contact an administrator.");
        }

        loginRateLimiter.reset(ipAddress);
        auditService.record(user.getId(), EventType.AUTH,
                "login_success username=" + username, ipAddress);
        return user;
    }

    /** Returns a user by ID or throws ServiceException if not found. */
    public User findById(long id) throws ServiceException, SQLException {
        return userDao.findById(id)
                .orElseThrow(() -> new ServiceException("User not found"));
    }

    /** Returns all users (admin). */
    public List<User> findAll() throws SQLException {
        return userDao.findAll();
    }

    /** Searches users by username or email (admin). */
    public List<User> search(String query) throws SQLException {
        return userDao.search(query);
    }

    /** Returns total user count (admin dashboard). */
    public int countAll() throws SQLException {
        return userDao.countAll();
    }

    /**
     * Updates profile fields (username, email) for a user.
     * Password change is handled separately by changePassword().
     */
    public void updateProfile(long userId, String username, String email, String ipAddress)
            throws ServiceException, SQLException {

        if (!ValidationUtil.isValidUsername(username)) {
            throw new ServiceException("Invalid username format");
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new ServiceException("Invalid email address");
        }

        User user = findById(userId);
        // Check if the new username/email conflicts with another user
        Optional<User> byUsername = userDao.findByUsername(username);
        if (byUsername.isPresent() && byUsername.get().getId() != userId) {
            throw new ServiceException("Username already taken");
        }
        Optional<User> byEmail = userDao.findByEmail(email);
        if (byEmail.isPresent() && byEmail.get().getId() != userId) {
            throw new ServiceException("Email already registered");
        }

        user.setUsername(username);
        user.setEmail(email);
        userDao.update(user);
        auditService.record(userId, EventType.AUTH,
                "profile_updated userId=" + userId, ipAddress);
    }

    /**
     * Changes a user's password after verifying the current password.
     */
    public void changePassword(long userId, char[] currentPassword, char[] newPassword,
                               String ipAddress) throws ServiceException, SQLException {

        User user = findById(userId);
        boolean valid = PasswordUtil.verify(currentPassword, user.getPasswordHash());
        Arrays.fill(currentPassword, '\0');

        if (!valid) {
            throw new ServiceException("Current password is incorrect");
        }
        if (!PasswordUtil.meetsPolicy(newPassword)) {
            throw new ServiceException("New password must be 8–128 characters");
        }

        user.setPasswordHash(PasswordUtil.hash(newPassword));
        Arrays.fill(newPassword, '\0');
        userDao.update(user);
        auditService.record(userId, EventType.AUTH,
                "password_changed userId=" + userId, ipAddress);
    }

    /**
     * Sets a new password directly (used during password reset flow after token validation).
     * Caller must have already validated the reset token.
     */
    public void setPasswordDirect(long userId, char[] newPassword, String ipAddress)
            throws ServiceException, SQLException {

        if (!PasswordUtil.meetsPolicy(newPassword)) {
            throw new ServiceException("Password must be 8–128 characters");
        }
        User user = findById(userId);
        user.setPasswordHash(PasswordUtil.hash(newPassword));
        Arrays.fill(newPassword, '\0');
        userDao.update(user);
        auditService.record(userId, EventType.AUTH,
                "password_reset_complete userId=" + userId, ipAddress);
    }
}
