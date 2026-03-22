package com.loosenotes.dao;

import com.loosenotes.db.Database;
import com.loosenotes.model.ActivityLogEntry;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao {
    public void log(Long actorUserId, String actionType, String details) throws SQLException {
        String sql = "INSERT INTO activity_logs (actor_user_id, action_type, details) VALUES (?, ?, ?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            if (actorUserId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setLong(1, actorUserId);
            }
            statement.setString(2, actionType);
            statement.setString(3, details);
            statement.executeUpdate();
        }
    }

    public List<ActivityLogEntry> listRecent(int limit) throws SQLException {
        String sql =
            "SELECT a.id, COALESCE(u.username, 'System') AS actor_username, a.action_type, a.details, a.created_at "
                + "FROM activity_logs a "
                + "LEFT JOIN users u ON u.id = a.actor_user_id "
                + "ORDER BY a.created_at DESC, a.id DESC "
                + "LIMIT ?";
        List<ActivityLogEntry> entries = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    entries.add(new ActivityLogEntry(
                        rs.getLong("id"),
                        rs.getString("actor_username"),
                        rs.getString("action_type"),
                        rs.getString("details"),
                        rs.getString("created_at")
                    ));
                }
            }
        }
        return entries;
    }
}
