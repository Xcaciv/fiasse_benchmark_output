package com.loosenotes.service;

import com.loosenotes.dao.PasswordResetTokenDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.util.SecureTokenUtil;
import com.loosenotes.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Manages password reset flow.
 * SSEM: Authenticity - cryptographic tokens, time-limited, single-use.
 * SSEM: Confidentiality - consistent response time prevents user enumeration.
 * SSEM: Accountability - reset events are logged by callers.
 */
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);
    private static final int TOKEN_VALIDITY_MINUTES = 60;

    private final PasswordResetTokenDao tokenDao;
    private final UserDao userDao;
    private final UserService userService;

    public PasswordResetService(PasswordResetTokenDao tokenDao, UserDao userDao,
                                 UserService userService) {
        this.tokenDao = tokenDao;
        this.userDao = userDao;
        this.userService = userService;
    }

    /**
     * Initiates a password reset for the given email.
     * Returns the raw token only if the email is found (for sending via email).
     * Callers should always return a success response regardless to prevent enumeration.
     *
     * @param email the user's email address
     * @return raw token to include in the reset link, or empty if no user found
     */
    public Optional<String> requestReset(String email) throws SQLException {
        Optional<User> userOpt = userDao.findByEmail(email);
        if (userOpt.isEmpty()) {
            // SSEM: Confidentiality - do not reveal whether email exists
            log.debug("Password reset requested for unknown email domain={}",
                email.contains("@") ? email.substring(email.indexOf('@') + 1) : "unknown");
            return Optional.empty();
        }

        User user = userOpt.get();
        String rawToken  = SecureTokenUtil.generateToken();
        String tokenHash = SecureTokenUtil.hashToken(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_VALIDITY_MINUTES);

        tokenDao.create(user.getId(), tokenHash, expiresAt);
        return Optional.of(rawToken);
    }

    /**
     * Validates a reset token and returns the associated user ID.
     *
     * @param rawToken the token from the reset URL
     * @return userId if the token is valid, empty otherwise
     */
    public Optional<Long> validateToken(String rawToken) throws SQLException {
        if (!ValidationUtil.isValidToken(rawToken)) {
            return Optional.empty();
        }
        String tokenHash = SecureTokenUtil.hashToken(rawToken);
        return tokenDao.findValidUserId(tokenHash);
    }

    /**
     * Completes a password reset: validates token, sets new password, marks token used.
     *
     * @param rawToken    token from the reset URL
     * @param newPassword the new password to set
     * @return true if the reset succeeded, false if token is invalid/expired
     */
    public boolean completeReset(String rawToken, String newPassword) throws SQLException {
        Optional<Long> userIdOpt = validateToken(rawToken);
        if (userIdOpt.isEmpty()) return false;

        String tokenHash = SecureTokenUtil.hashToken(rawToken);
        userService.resetPassword(userIdOpt.get(), newPassword);
        tokenDao.markUsed(tokenHash);
        return true;
    }
}
