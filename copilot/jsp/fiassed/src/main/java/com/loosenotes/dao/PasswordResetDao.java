package com.loosenotes.dao;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public class PasswordResetDao {

    public void insert(Connection conn, long userId, String tokenHash, LocalDateTime expiresAt) throws SQLException {
        // Invalidate old tokens first
        invalidateForUser(conn, userId);
        String sql = "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, tokenHash);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    public Optional<Long> findValidUserId(Connection conn, String tokenHash) throws SQLException {
        String sql = "SELECT user_id FROM password_reset_tokens WHERE token_hash = ? AND used = FALSE AND expires_at > CURRENT_TIMESTAMP";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(rs.getLong("user_id")) : Optional.empty();
            }
        }
    }

    public void markUsed(Connection conn, String tokenHash) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used = TRUE WHERE token_hash = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            ps.executeUpdate();
        }
    }

    public void invalidateForUser(Connection conn, long userId) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used = TRUE WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }
}
