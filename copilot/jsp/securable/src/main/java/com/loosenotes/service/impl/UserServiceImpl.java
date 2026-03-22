package com.loosenotes.service.impl;

import com.loosenotes.audit.AuditLogger;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.service.ServiceException;
import com.loosenotes.service.UserService;
import com.loosenotes.util.PasswordUtil;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public final class UserServiceImpl implements UserService {

    private static final Logger log = LoggerFactory.getLogger(UserServiceImpl.class);

    private final UserDao userDao;
    private final AuditLogger auditLogger;

    public UserServiceImpl(UserDao userDao, AuditLogger auditLogger) {
        this.userDao     = userDao;
        this.auditLogger = auditLogger;
    }

    @Override
    public User register(String username, String email, String password) {
        // Trust boundary: validate and canonicalize inputs
        String cleanUsername = ValidationUtil.validateUsername(username);
        String cleanEmail    = ValidationUtil.validateEmail(email);
        ValidationUtil.validatePassword(password);

        if (userDao.existsByUsername(cleanUsername)) {
            throw new ServiceException(ServiceException.ErrorCode.DUPLICATE_USERNAME,
                    "Username already taken");
        }
        if (userDao.existsByEmail(cleanEmail)) {
            throw new ServiceException(ServiceException.ErrorCode.DUPLICATE_EMAIL,
                    "Email address already registered");
        }

        String hash = PasswordUtil.hash(password);
        User newUser = new User(0, cleanUsername, cleanEmail, hash, "USER", LocalDateTime.now());
        long id = userDao.insert(newUser);

        auditLogger.log(id, cleanUsername, "USER_REGISTER", "USER", String.valueOf(id),
                null, "SUCCESS", "New user registered");
        log.info("User registered: {}", cleanUsername);

        return userDao.findById(id).orElseThrow(() ->
                new ServiceException(ServiceException.ErrorCode.NOT_FOUND, "User not found after insert"));
    }

    @Override
    public Optional<User> authenticate(String username, String password) {
        // Do not log password or hash (Confidentiality)
        Optional<User> userOpt = userDao.findByUsername(username);
        if (userOpt.isEmpty()) {
            auditLogger.log(null, username, "LOGIN_FAILED", "USER", null,
                    null, "FAILURE", "Unknown username");
            return Optional.empty();
        }
        User user = userOpt.get();
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            auditLogger.log(user.getId(), username, "LOGIN_FAILED", "USER",
                    String.valueOf(user.getId()), null, "FAILURE", "Bad password");
            return Optional.empty();
        }
        auditLogger.log(user.getId(), username, "LOGIN_SUCCESS", "USER",
                String.valueOf(user.getId()), null, "SUCCESS", null);
        return Optional.of(user);
    }

    @Override
    public Optional<User> findById(long id) {
        return userDao.findById(id);
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userDao.findByUsername(username);
    }

    @Override
    public void updateProfile(long userId, String username, String email) {
        String cleanUsername = ValidationUtil.validateUsername(username);
        String cleanEmail    = ValidationUtil.validateEmail(email);

        Optional<User> existing = userDao.findByUsername(cleanUsername);
        if (existing.isPresent() && existing.get().getId() != userId) {
            throw new ServiceException(ServiceException.ErrorCode.DUPLICATE_USERNAME,
                    "Username already taken");
        }
        Optional<User> byEmail = userDao.findByEmail(cleanEmail);
        if (byEmail.isPresent() && byEmail.get().getId() != userId) {
            throw new ServiceException(ServiceException.ErrorCode.DUPLICATE_EMAIL,
                    "Email already in use");
        }

        User user = userDao.findById(userId)
                .orElseThrow(() -> new ServiceException(ServiceException.ErrorCode.NOT_FOUND, "User not found"));
        User updated = new User(userId, cleanUsername, cleanEmail,
                user.getPasswordHash(), user.getRole(), user.getCreatedAt());
        userDao.update(updated);

        auditLogger.log(userId, cleanUsername, "PROFILE_UPDATE", "USER",
                String.valueOf(userId), null, "SUCCESS", null);
    }

    @Override
    public void changePassword(long userId, String currentPassword, String newPassword) {
        ValidationUtil.validatePassword(newPassword);
        User user = userDao.findById(userId)
                .orElseThrow(() -> new ServiceException(ServiceException.ErrorCode.NOT_FOUND, "User not found"));
        if (!PasswordUtil.verify(currentPassword, user.getPasswordHash())) {
            throw new ServiceException(ServiceException.ErrorCode.INVALID_CREDENTIALS,
                    "Current password is incorrect");
        }
        userDao.updatePassword(userId, PasswordUtil.hash(newPassword));
        auditLogger.log(userId, user.getUsername(), "PASSWORD_CHANGE", "USER",
                String.valueOf(userId), null, "SUCCESS", null);
    }

    @Override
    public List<User> findAll() {
        return userDao.findAll();
    }

    @Override
    public List<User> searchUsers(String query) {
        String cleanQuery = ValidationUtil.requireNonBlank(query, "Search query", 100);
        return userDao.searchByUsernameOrEmail(cleanQuery);
    }

    @Override
    public int countAll() {
        return userDao.countAll();
    }

    @Override
    public int countNotesByUser(long userId) {
        return userDao.countNotesByUserId(userId);
    }
}
