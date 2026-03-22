package com.loosenotes.dao;

import com.loosenotes.model.ActivityLog;
import com.loosenotes.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {

    public void log(Integer userId, String action, String details) {
        String sql = "INSERT INTO activity_log (user_id, action, details) VALUES (?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (userId != null) {
                pstmt.setInt(1, userId);
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setString(2, action);
            pstmt.setString(3, details);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            // Logging failure should not break the application
            System.err.println("Failed to log activity: " + e.getMessage());
        }
    }

    public List<ActivityLog> findRecent(int limit) throws SQLException {
        String sql = "SELECT al.id, al.user_id, al.action, al.details, al.created_at, u.username " +
                     "FROM activity_log al LEFT JOIN users u ON al.user_id = u.id " +
                     "ORDER BY al.created_at DESC LIMIT ?";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }
        return logs;
    }

    public List<ActivityLog> findByUserId(int userId) throws SQLException {
        String sql = "SELECT al.id, al.user_id, al.action, al.details, al.created_at, u.username " +
                     "FROM activity_log al LEFT JOIN users u ON al.user_id = u.id " +
                     "WHERE al.user_id = ? ORDER BY al.created_at DESC";
        List<ActivityLog> logs = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    logs.add(mapRow(rs));
                }
            }
        }
        return logs;
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getInt("id"));
        int userId = rs.getInt("user_id");
        if (!rs.wasNull()) {
            log.setUserId(userId);
        }
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        log.setUsername(rs.getString("username"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            try {
                log.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
            } catch (Exception e) {
                log.setCreatedAt(LocalDateTime.now());
            }
        }
        return log;
    }
}
