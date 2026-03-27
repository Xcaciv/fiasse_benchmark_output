package com.loosenotes.service;

import com.loosenotes.dao.SessionDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.util.AuditLogger;
import com.loosenotes.util.DatabaseManager;
import com.loosenotes.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.Optional;

public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;
    private final UserDao userDao = new UserDao();
    private final SessionDao sessionDao = new SessionDao();
    private final AuditLogger auditLogger = AuditLogger.getInstance();

    public Optional<User> authenticate(String username, String password, String ip, String sessionId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<User> userOpt = userDao.findByUsername(conn, username);
            if (userOpt.isEmpty()) {
                auditLogger.log("LOGIN_FAILURE", null, null, ip, "FAILURE", sessionId, "unknown_user");
                return Optional.empty();
            }
            User user = userOpt.get();
            if (user.isLocked()) {
                auditLogger.log("LOGIN_FAILURE", user.getId(), null, ip, "FAILURE", sessionId, "account_locked");
                return Optional.empty();
            }
            if (!SecurityUtils.verifyPassword(password, user.getPasswordHash())) {
                int attempts = user.getFailedLoginAttempts() + 1;
                LocalDateTime lockout = attempts >= MAX_FAILED_ATTEMPTS ? LocalDateTime.now().plusMinutes(LOCKOUT_MINUTES) : null;
                userDao.updateFailedLoginAttempts(conn, user.getId(), attempts, lockout);
                auditLogger.log("LOGIN_FAILURE", user.getId(), null, ip, "FAILURE", sessionId, "bad_password");
                return Optional.empty();
            }
            userDao.updateFailedLoginAttempts(conn, user.getId(), 0, null);
            auditLogger.log("LOGIN_SUCCESS", user.getId(), null, ip, "SUCCESS", sessionId, null);
            return Optional.of(user);
        } catch (Exception e) {
            logger.error("Authentication error", e);
            return Optional.empty();
        }
    }

    public boolean register(String username, String email, String password, String ip) {
        if (username == null || username.length() < 3 || username.length() > 50) return false;
        if (email == null || email.length() > 255 || !email.contains("@")) return false;
        if (password == null || password.length() < 12 || password.length() > 64) return false;
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            if (userDao.findByUsername(conn, username).isPresent()) return false;
            if (userDao.findByEmail(conn, email).isPresent()) return false;
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPasswordHash(SecurityUtils.hashPassword(password));
            user.setRole("USER");
            long id = userDao.insert(conn, user);
            auditLogger.log("REGISTER", id, null, ip, "SUCCESS", null, null);
            return true;
        } catch (Exception e) {
            logger.error("Registration error", e);
            return false;
        }
    }

    public boolean changePassword(long userId, String oldPassword, String newPassword, String ip, String sessionId) {
        if (newPassword == null || newPassword.length() < 12 || newPassword.length() > 64) return false;
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Optional<User> userOpt = userDao.findById(conn, userId);
            if (userOpt.isEmpty()) return false;
            User user = userOpt.get();
            if (!SecurityUtils.verifyPassword(oldPassword, user.getPasswordHash())) return false;
            userDao.updatePassword(conn, userId, SecurityUtils.hashPassword(newPassword));
            sessionDao.deleteByUserId(conn, userId);
            auditLogger.log("PASSWORD_CHANGE", userId, null, ip, "SUCCESS", sessionId, null);
            return true;
        } catch (Exception e) {
            logger.error("Password change error", e);
            return false;
        }
    }

    public Optional<User> findById(long userId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return userDao.findById(conn, userId);
        } catch (Exception e) {
            logger.error("Error finding user by id", e);
            return Optional.empty();
        }
    }
}
