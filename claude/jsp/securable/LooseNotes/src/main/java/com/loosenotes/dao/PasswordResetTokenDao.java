package com.loosenotes.dao;

import java.sql.*;
import java.time.Instant;
import java.util.Optional;

/**
 * Data access for the password_reset_tokens table.
 *
 * SSEM notes:
 * - Authenticity: stores token_hash (SHA-256 of the raw token), not the raw token.
 *   The raw token is sent only via email; never persisted.
 * - Integrity: expiry and used-flag are checked atomically in findValidToken.
 * - Resilience: markUsed is separate from delete to enable forensic review.
 */
public class PasswordResetTokenDao {

    private final DatabaseManager db;

    public PasswordResetTokenDao(DatabaseManager db) {
        this.db = db;
    }

    /**
     * Stores a hashed reset token for a user.
     * The raw token is never stored; only its SHA-256 hex hash.
     *
     * @param userId    the user requesting the reset
     * @param tokenHash SHA-256 hex hash of the raw token
     * @param expiresAt expiry timestamp
     */
    public void insert(long userId, String tokenHash, Instant expiresAt) throws SQLException {
        // Invalidate any existing unused tokens for this user first
        invalidateExistingTokens(userId);
        String sql = "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, Timestamp.from(expiresAt));
            ps.executeUpdate();
        }
    }

    /**
     * Finds a valid (unused, unexpired) token record by its hash.
     * Returns the associated userId if found.
     */
    public Optional<Long> findValidUserIdByTokenHash(String tokenHash) throws SQLException {
        String sql = "SELECT user_id FROM password_reset_tokens "
                + "WHERE token_hash = ? AND used = FALSE AND expires_at > CURRENT_TIMESTAMP";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong("user_id")) : Optional.empty();
            }
        }
    }

    /**
     * Marks a token as used so it cannot be replayed.
     *
     * @param tokenHash the hash of the consumed token
     */
    public void markUsed(String tokenHash) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used = TRUE WHERE token_hash = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.executeUpdate();
        }
    }

    private void invalidateExistingTokens(long userId) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used = TRUE "
                + "WHERE user_id = ? AND used = FALSE";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }
}
