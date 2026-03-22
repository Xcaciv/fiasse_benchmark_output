package com.loosenotes.dao;

import com.loosenotes.model.PasswordResetToken;
import com.loosenotes.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;

public class PasswordResetTokenDAO {

    public PasswordResetToken findByToken(String token) throws SQLException {
        String sql = "SELECT prt.id, prt.user_id, prt.token, prt.expires_at, prt.used, prt.created_at, " +
                     "u.username, u.email FROM password_reset_tokens prt " +
                     "JOIN users u ON prt.user_id = u.id WHERE prt.token = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int create(int userId, String token, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO password_reset_tokens (user_id, token, expires_at) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, token);
            pstmt.setString(3, expiresAt.toString().replace("T", " "));
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void markUsed(String token) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used = 1 WHERE token = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            pstmt.executeUpdate();
        }
    }

    public void deleteByUserId(int userId) throws SQLException {
        String sql = "DELETE FROM password_reset_tokens WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            pstmt.executeUpdate();
        }
    }

    private PasswordResetToken mapRow(ResultSet rs) throws SQLException {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(rs.getInt("id"));
        token.setUserId(rs.getInt("user_id"));
        token.setToken(rs.getString("token"));
        token.setUsed(rs.getInt("used") == 1);
        token.setUsername(rs.getString("username"));
        token.setEmail(rs.getString("email"));
        String expiresAt = rs.getString("expires_at");
        if (expiresAt != null) {
            try {
                token.setExpiresAt(LocalDateTime.parse(expiresAt.replace(" ", "T")));
            } catch (Exception e) {
                token.setExpiresAt(LocalDateTime.now().minusHours(1));
            }
        }
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            try {
                token.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
            } catch (Exception e) {
                token.setCreatedAt(LocalDateTime.now());
            }
        }
        return token;
    }
}
