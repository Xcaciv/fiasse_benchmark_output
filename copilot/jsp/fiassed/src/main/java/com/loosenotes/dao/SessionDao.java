package com.loosenotes.dao;

import java.sql.*;

public class SessionDao {

    public void insert(Connection conn, long userId, String sessionIdHash) throws SQLException {
        String sql = "INSERT INTO user_sessions (user_id, session_id_hash) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, sessionIdHash);
            ps.executeUpdate();
        }
    }

    public void deleteByUserId(Connection conn, long userId) throws SQLException {
        String sql = "DELETE FROM user_sessions WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public void deleteBySessionHash(Connection conn, String sessionIdHash) throws SQLException {
        String sql = "DELETE FROM user_sessions WHERE session_id_hash = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionIdHash);
            ps.executeUpdate();
        }
    }

    public void updateLastAccessed(Connection conn, String sessionIdHash) throws SQLException {
        String sql = "UPDATE user_sessions SET last_accessed_at = CURRENT_TIMESTAMP WHERE session_id_hash = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionIdHash);
            ps.executeUpdate();
        }
    }
}
