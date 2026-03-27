package com.loosenotes.dao;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PasswordResetTokenDAO {
    private static final Logger LOGGER = Logger.getLogger(PasswordResetTokenDAO.class.getName());

    public boolean create(int userId, String token, Timestamp expiresAt) {
        // Invalidate any existing tokens for this user first
        invalidateForUser(userId);
        String sql = "INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, token);
            ps.setTimestamp(3, expiresAt);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating password reset token", e);
        }
        return false;
    }

    /**
     * Returns the userId associated with the token if valid (not used, not expired), else -1.
     */
    public int validateToken(String token) {
        String sql = "SELECT user_id, used, expires_at FROM password_reset_tokens WHERE token = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int used = rs.getInt("used");
                    Timestamp expiresAt = rs.getTimestamp("expires_at");
                    if (used == 1) return -1;
                    if (expiresAt != null && expiresAt.before(new Timestamp(System.currentTimeMillis()))) return -1;
                    return rs.getInt("user_id");
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error validating password reset token", e);
        }
        return -1;
    }

    public boolean markUsed(String token) {
        String sql = "UPDATE password_reset_tokens SET used = 1 WHERE token = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error marking token as used", e);
        }
        return false;
    }

    private void invalidateForUser(int userId) {
        String sql = "UPDATE password_reset_tokens SET used = 1 WHERE user_id = ? AND used = 0";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error invalidating old tokens", e);
        }
    }
}
