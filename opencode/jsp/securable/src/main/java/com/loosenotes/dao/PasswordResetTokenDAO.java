package com.loosenotes.dao;

import com.loosenotes.model.PasswordResetToken;
import com.loosenotes.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Optional;

public class PasswordResetTokenDAO {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetTokenDAO.class);
    
    public Long create(PasswordResetToken token) throws SQLException {
        String sql = "INSERT INTO password_reset_tokens (user_id, token, created_at, expires_at, used) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, token.getUserId());
            stmt.setString(2, token.getToken());
            stmt.setTimestamp(3, Timestamp.valueOf(token.getCreatedAt()));
            stmt.setTimestamp(4, Timestamp.valueOf(token.getExpiresAt()));
            stmt.setBoolean(5, token.isUsed());
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return null;
    }
    
    public Optional<PasswordResetToken> findByToken(String token) throws SQLException {
        String sql = "SELECT * FROM password_reset_tokens WHERE token = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, token);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToToken(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    public void markAsUsed(String token) throws SQLException {
        String sql = "UPDATE password_reset_tokens SET used = TRUE WHERE token = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, token);
            stmt.executeUpdate();
        }
    }
    
    public void deleteByUserId(Long userId) throws SQLException {
        String sql = "DELETE FROM password_reset_tokens WHERE user_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        }
    }
    
    public void deleteExpired() throws SQLException {
        String sql = "DELETE FROM password_reset_tokens WHERE expires_at < CURRENT_TIMESTAMP OR used = TRUE";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.executeUpdate();
        }
    }
    
    private PasswordResetToken mapResultSetToToken(ResultSet rs) throws SQLException {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(rs.getLong("id"));
        token.setUserId(rs.getLong("user_id"));
        token.setToken(rs.getString("token"));
        token.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        token.setExpiresAt(rs.getTimestamp("expires_at").toLocalDateTime());
        token.setUsed(rs.getBoolean("used"));
        return token;
    }
}
