package com.loosenotes.dao;

import com.loosenotes.model.ActivityLog;
import com.loosenotes.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {

    private final UserDAO userDAO = new UserDAO();

    public boolean logActivity(ActivityLog activity) {
        String sql = "INSERT INTO activity_log (user_id, action, entity_type, entity_id, details, ip_address) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            if (activity.getUserId() != null) {
                stmt.setLong(1, activity.getUserId());
            } else {
                stmt.setNull(1, Types.BIGINT);
            }
            stmt.setString(2, activity.getAction());
            stmt.setString(3, activity.getEntityType());
            if (activity.getEntityId() != null) {
                stmt.setLong(4, activity.getEntityId());
            } else {
                stmt.setNull(4, Types.BIGINT);
            }
            stmt.setString(5, activity.getDetails());
            stmt.setString(6, activity.getIpAddress());
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    activity.setId(rs.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public List<ActivityLog> findRecent(int limit) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT a.*, u.username FROM activity_log a LEFT JOIN users u ON a.user_id = u.id ORDER BY a.created_at DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ActivityLog log = mapResultSetToActivityLog(rs);
                User user = new User();
                user.setUsername(rs.getString("username"));
                log.setUser(user);
                logs.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public List<ActivityLog> findByUserId(Long userId, int limit) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT a.*, u.username FROM activity_log a LEFT JOIN users u ON a.user_id = u.id WHERE a.user_id = ? ORDER BY a.created_at DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ActivityLog log = mapResultSetToActivityLog(rs);
                User user = new User();
                user.setUsername(rs.getString("username"));
                log.setUser(user);
                logs.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    public List<ActivityLog> findByAction(String action, int limit) {
        List<ActivityLog> logs = new ArrayList<>();
        String sql = "SELECT a.*, u.username FROM activity_log a LEFT JOIN users u ON a.user_id = u.id WHERE a.action = ? ORDER BY a.created_at DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, action);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ActivityLog log = mapResultSetToActivityLog(rs);
                User user = new User();
                user.setUsername(rs.getString("username"));
                log.setUser(user);
                logs.add(log);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return logs;
    }

    private ActivityLog mapResultSetToActivityLog(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getLong("id"));
        
        Long userId = rs.getLong("user_id");
        if (!rs.wasNull()) {
            log.setUserId(userId);
        }
        
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        
        Long entityId = rs.getLong("entity_id");
        if (!rs.wasNull()) {
            log.setEntityId(entityId);
        }
        
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        log.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        return log;
    }
}
