package com.loosenotes.dao;

import com.loosenotes.db.ConnectionFactory;
import com.loosenotes.model.ActivityLog;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDao extends BaseDao {
    public void log(Long actorUserId, String action, String details) {
        String sql = "INSERT INTO activity_logs(actor_user_id, action, details, created_at) VALUES (?, ?, ?, ?)";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (actorUserId == null) {
                statement.setNull(1, java.sql.Types.INTEGER);
            } else {
                statement.setLong(1, actorUserId);
            }
            statement.setString(2, action);
            statement.setString(3, details);
            statement.setString(4, now());
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to write activity log entry.", ex);
        }
    }

    public List<ActivityLog> recent(int limit) {
        String sql = "SELECT * FROM activity_logs ORDER BY created_at DESC LIMIT ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, limit);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<ActivityLog> logs = new ArrayList<>();
                while (resultSet.next()) {
                    ActivityLog log = new ActivityLog();
                    log.setId(resultSet.getLong("id"));
                    long actorId = resultSet.getLong("actor_user_id");
                    log.setActorUserId(resultSet.wasNull() ? null : actorId);
                    log.setAction(resultSet.getString("action"));
                    log.setDetails(resultSet.getString("details"));
                    log.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
                    logs.add(log);
                }
                return logs;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to read activity logs.", ex);
        }
    }
}
