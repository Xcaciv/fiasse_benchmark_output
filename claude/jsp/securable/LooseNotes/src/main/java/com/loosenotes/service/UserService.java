package com.loosenotes.service;

import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Business logic for user management.
 * SSEM: Authenticity - login, lockout, and credential management.
 * SSEM: Confidentiality - passwords hashed before storage, never logged.
 * SSEM: Integrity - validates all inputs before DAO calls.
 */
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    /** After this many failed attempts, account is locked. */
    private static final int MAX_FAILED_ATTEMPTS = 5;
    /** Duration of account lock after max failures. */
    private static final int LOCK_MINUTES = 15;

    private final UserDao userDao;

    public UserService(UserDao userDao) {
        this.userDao = userDao;
    }

    /**
     * Result of an authentication attempt.
     * SSEM: Analyzability - explicit result type avoids boolean ambiguity.
     */
    public enum AuthResult {
        SUCCESS, INVALID_CREDENTIALS, ACCOUNT_LOCKED, NOT_FOUND
    }

    /**
     * Registers a new user.
     * Trust boundary: validates all inputs before hashing and storage.
     *
     * @return the created User, or empty if username/email already taken
     * @throws IllegalArgumentException if inputs fail validation
     * @throws SQLException on database error
     */
    public Optional<User> register(String username, String email,
                                    String rawPassword) throws SQLException {
        validateRegistrationInputs(username, email, rawPassword);

        if (userDao.findByUsername(username).isPresent()) {
            return Optional.empty();
        }
        if (userDao.findByEmail(email).isPresent()) {
            return Optional.empty();
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(PasswordUtil.hash(rawPassword));
        long id = userDao.create(user);
        user.setId(id);
        return Optional.of(user);
    }

    /**
     * Authenticates a user by username and password.
     * SSEM: Authenticity - BCrypt verify; account lockout after N failures.
     *
     * @return AuthResult indicating the outcome
     */
    public AuthResult authenticate(String username, String rawPassword) throws SQLException {
        Optional<User> opt = userDao.findByUsername(username);
        if (opt.isEmpty()) {
            return AuthResult.NOT_FOUND;
        }
        User user = opt.get();
        if (user.isLocked()) {
            return AuthResult.ACCOUNT_LOCKED;
        }
        if (!PasswordUtil.verify(rawPassword, user.getPasswordHash())) {
            recordFailedLogin(user);
            return AuthResult.INVALID_CREDENTIALS;
        }
        userDao.resetFailedLogins(user.getId());
        return AuthResult.SUCCESS;
    }

    /** Returns a user by username (for session population after login). */
    public Optional<User> findByUsername(String username) throws SQLException {
        return userDao.findByUsername(username);
    }

    /** Returns a user by ID. */
    public Optional<User> findById(long id) throws SQLException {
        return userDao.findById(id);
    }

    /**
     * Updates profile fields (username, email).
     * Does not allow role changes via this method.
     */
    public boolean updateProfile(long userId, String newUsername,
                                  String newEmail) throws SQLException {
        if (!ValidationUtil.isValidUsername(newUsername)) {
            throw new IllegalArgumentException("Invalid username format");
        }
        if (!ValidationUtil.isValidEmail(newEmail)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        Optional<User> conflictUser = userDao.findByUsername(newUsername);
        if (conflictUser.isPresent() && conflictUser.get().getId() != userId) {
            return false; // Username taken by another user
        }
        userDao.updateProfile(userId, newUsername, newEmail);
        return true;
    }

    /**
     * Changes a user's password after verifying the current password.
     */
    public boolean changePassword(long userId, String currentPassword,
                                   String newPassword) throws SQLException {
        Optional<User> opt = userDao.findById(userId);
        if (opt.isEmpty()) return false;

        User user = opt.get();
        if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) {
            return false;
        }
        if (!PasswordUtil.meetsComplexity(newPassword)) {
            throw new IllegalArgumentException(PasswordUtil.getComplexityRequirements());
        }
        userDao.updatePasswordHash(userId, PasswordUtil.hash(newPassword));
        return true;
    }

    /** Resets password directly (used after validating a reset token). */
    public void resetPassword(long userId, String newPassword) throws SQLException {
        if (!PasswordUtil.meetsComplexity(newPassword)) {
            throw new IllegalArgumentException(PasswordUtil.getComplexityRequirements());
        }
        userDao.updatePasswordHash(userId, PasswordUtil.hash(newPassword));
    }

    /** Returns a paginated list of all users (admin use only). */
    public java.util.List<User> listUsers(int page, int pageSize) throws SQLException {
        return userDao.findAll(page, pageSize);
    }

    /** Searches users by username or email (admin use only). */
    public java.util.List<User> searchUsers(String query, int limit) throws SQLException {
        return userDao.search(query, limit);
    }

    /** Returns total user count (admin use only). */
    public long countUsers() throws SQLException {
        return userDao.count();
    }

    private void recordFailedLogin(User user) throws SQLException {
        LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCK_MINUTES);
        userDao.recordFailedLogin(user.getId(), MAX_FAILED_ATTEMPTS, lockUntil);
    }

    private void validateRegistrationInputs(String username, String email, String password) {
        if (!ValidationUtil.isValidUsername(username)) {
            throw new IllegalArgumentException("Invalid username: " + ValidationUtil.class.getSimpleName());
        }
        if (!ValidationUtil.isValidEmail(email)) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (!PasswordUtil.meetsComplexity(password)) {
            throw new IllegalArgumentException(PasswordUtil.getComplexityRequirements());
        }
    }
}
