package com.loosenotes.dao;

import java.sql.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AuditLogDAO {
    private static final Logger LOGGER = Logger.getLogger(AuditLogDAO.class.getName());

    public void log(Integer userId, String action, String details) {
        String sql = "INSERT INTO activity_log (user_id, action, details) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, action);
            ps.setString(3, details);
            ps.executeUpdate();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error writing activity log", e);
        }
    }

    public List<Map<String, Object>> getRecent(int limit) {
        List<Map<String, Object>> logs = new ArrayList<>();
        String sql = "SELECT a.id, a.action, a.details, a.created_at, u.username " +
                     "FROM activity_log a LEFT JOIN users u ON a.user_id = u.id " +
                     "ORDER BY a.created_at DESC LIMIT ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("id", rs.getInt("id"));
                    row.put("action", rs.getString("action"));
                    row.put("details", rs.getString("details"));
                    row.put("createdAt", rs.getString("created_at"));
                    row.put("username", rs.getString("username"));
                    logs.add(row);
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error fetching activity logs", e);
        }
        return logs;
    }
}
