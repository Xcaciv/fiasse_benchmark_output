package com.loosenotes.dao;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Data access layer for password reset tokens.
 * SSEM: Authenticity - tokens are stored as SHA-256 hashes, not plaintext.
 * SSEM: Integrity - expired and used tokens are rejected.
 */
public class PasswordResetTokenDao {

    private final DataSource dataSource;

    public PasswordResetTokenDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Creates a new password reset token record.
     *
     * @param userId    user requesting the reset
     * @param tokenHash SHA-256 hash of the raw token (raw token goes in the email)
     * @param expiresAt expiry timestamp
     */
    public void create(long userId, String tokenHash, LocalDateTime expiresAt) throws SQLException {
        // Invalidate any previous unused tokens for this user
        invalidateExistingTokens(userId);
        String sql = "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) "
            + "VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    /**
     * Finds a valid (unused, non-expired) token record by token hash.
     * Returns the associated user ID if valid.
     */
    public Optional<Long> findValidUserId(String tokenHash) throws SQLException {
        String sql = "SELECT user_id FROM password_reset_tokens "
            + "WHERE token_hash = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(rs.getLong("user_id"));
            }
        }
        return Optional.empty();
    }

    /**
     * Marks a token as used so it cannot be reused.
     * SSEM: Authenticity - single-use enforcement.
     */
    public void markUsed(String tokenHash) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP "
            + "WHERE token_hash = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.executeUpdate();
        }
    }

    private void invalidateExistingTokens(long userId) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP "
            + "WHERE user_id = ? AND used_at IS NULL";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }
}
