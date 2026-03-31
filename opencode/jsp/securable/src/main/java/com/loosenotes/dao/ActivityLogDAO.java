package com.loosenotes.dao;

import com.loosenotes.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {
    private static final Logger logger = LoggerFactory.getLogger(ActivityLogDAO.class);
    
    public Long log(Long userId, String action, String details, String ipAddress) throws SQLException {
        String sql = "INSERT INTO activity_log (user_id, action, details, ip_address, created_at) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            if (userId != null) {
                stmt.setLong(1, userId);
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.setString(4, ipAddress);
            stmt.setTimestamp(5, Timestamp.valueOf(Timestamp.now().toLocalDateTime()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return null;
    }
    
    public List<ActivityLogEntry> findRecent(int limit) throws SQLException {
        String sql = "SELECT al.*, u.username FROM activity_log al " +
                     "LEFT JOIN users u ON al.user_id = u.id " +
                     "ORDER BY al.created_at DESC LIMIT ?";
        List<ActivityLogEntry> entries = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ActivityLogEntry entry = new ActivityLogEntry();
                    entry.setId(rs.getLong("id"));
                    
                    Long userId = rs.getLong("user_id");
                    if (!rs.wasNull()) {
                        entry.setUserId(userId);
                    }
                    
                    entry.setUsername(rs.getString("username"));
                    entry.setAction(rs.getString("action"));
                    entry.setDetails(rs.getString("details"));
                    entry.setIpAddress(rs.getString("ip_address"));
                    entry.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    entries.add(entry);
                }
            }
        }
        
        return entries;
    }
    
    public static class ActivityLogEntry {
        private Long id;
        private Long userId;
        private String username;
        private String action;
        private String details;
        private String ipAddress;
        private java.time.LocalDateTime createdAt;
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        public java.time.LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(java.time.LocalDateTime createdAt) { this.createdAt = createdAt; }
    }
}
