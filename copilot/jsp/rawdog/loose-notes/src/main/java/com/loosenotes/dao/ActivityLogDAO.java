package com.loosenotes.dao;

import com.loosenotes.model.ActivityLog;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {

    public void log(Integer userId, String action, String detail) {
        String sql = "INSERT INTO activity_log (user_id, action, detail, created_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (userId != null) ps.setInt(1, userId);
            else ps.setNull(1, Types.INTEGER);
            ps.setString(2, action);
            ps.setString(3, detail);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (Exception e) {
            // Log failure should not break the main flow
            e.printStackTrace();
        }
    }

    public List<ActivityLog> getRecentActivity(int limit) throws Exception {
        String sql = "SELECT * FROM activity_log ORDER BY created_at DESC LIMIT ?";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ActivityLog al = new ActivityLog();
                    al.setId(rs.getInt("id"));
                    int uid = rs.getInt("user_id");
                    al.setUserId(rs.wasNull() ? null : uid);
                    al.setAction(rs.getString("action"));
                    al.setDetail(rs.getString("detail"));
                    al.setCreatedAt(rs.getString("created_at"));
                    list.add(al);
                }
            }
        }
        return list;
    }
}
