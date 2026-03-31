package com.loosenotes.service;

import com.loosenotes.dao.PasswordResetTokenDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.model.AuditLog.EventType;
import com.loosenotes.model.User;
import com.loosenotes.util.SecureTokenUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;

/**
 * Manages the password reset token lifecycle.
 *
 * SSEM / ASVS alignment:
 * - ASVS V6.3 (Password Reset): time-limited, single-use tokens.
 * - Authenticity: only the hash of the token is stored; the raw token travels only via email.
 * - Resilience: previous tokens invalidated when a new one is issued.
 * - Confidentiality: user existence not disclosed in API responses to prevent enumeration.
 */
public class PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetService.class);

    private final PasswordResetTokenDao tokenDao;
    private final UserDao userDao;
    private final AuditService auditService;
    private final long tokenValiditySeconds;

    public PasswordResetService(PasswordResetTokenDao tokenDao, UserDao userDao,
                                AuditService auditService, long tokenValiditySeconds) {
        this.tokenDao             = tokenDao;
        this.userDao              = userDao;
        this.auditService         = auditService;
        this.tokenValiditySeconds = tokenValiditySeconds;
    }

    /**
     * Initiates a password reset by generating and storing a time-limited token.
     * Returns the raw token to be emailed (never stored).
     * If no account matches the email, the method returns normally to avoid enumeration.
     *
     * @param email      the submitted email address
     * @param ipAddress  client IP for audit
     * @return the raw reset token (caller sends via email), or null if no account found
     */
    public String initiateReset(String email, String ipAddress) throws SQLException {
        Optional<User> maybeUser = userDao.findByEmail(email);
        if (maybeUser.isEmpty()) {
            // Do not reveal whether the email is registered
            log.debug("Password reset requested for unknown email");
            return null;
        }

        User user = maybeUser.get();
        String rawToken  = SecureTokenUtil.generate();
        String tokenHash = hashToken(rawToken);
        Instant expiresAt = Instant.now().plusSeconds(tokenValiditySeconds);

        tokenDao.insert(user.getId(), tokenHash, expiresAt);
        auditService.record(user.getId(), EventType.AUTH,
                "password_reset_requested userId=" + user.getId(), ipAddress);
        return rawToken;
    }

    /**
     * Validates a reset token and returns the associated user ID if valid.
     * Does NOT mark the token as used – caller must call consumeToken after password change.
     *
     * @param rawToken the token from the reset URL
     * @return Optional of userId if the token is valid and unexpired
     */
    public Optional<Long> validateToken(String rawToken) throws SQLException {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        String tokenHash = hashToken(rawToken);
        return tokenDao.findValidUserIdByTokenHash(tokenHash);
    }

    /**
     * Marks a token as used so it cannot be replayed.
     * Called after the password has been successfully changed.
     *
     * @param rawToken the consumed token
     */
    public void consumeToken(String rawToken) throws SQLException {
        if (rawToken == null || rawToken.isBlank()) return;
        tokenDao.markUsed(hashToken(rawToken));
    }

    /**
     * Returns the SHA-256 hex hash of a token.
     * One-way: the raw token cannot be recovered from the hash.
     */
    private String hashToken(String rawToken) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(rawToken.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is always available on Java 17
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }
}
