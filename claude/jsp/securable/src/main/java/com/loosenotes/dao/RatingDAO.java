package com.loosenotes.dao;

import com.loosenotes.model.Rating;
import com.loosenotes.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RatingDAO {

    public void upsert(long noteId, long userId, int rating, String comment)
            throws SQLException {
        // H2 supports MERGE INTO
        String sql =
            "MERGE INTO ratings (note_id, user_id, rating, comment, created_at) " +
            "KEY (note_id, user_id) " +
            "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setLong(2, userId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.executeUpdate();
        }
    }

    public Rating findByNoteAndUser(long noteId, long userId) throws SQLException {
        String sql =
            "SELECT r.*, u.username AS rater_username " +
            "FROM ratings r JOIN users u ON u.id = r.user_id " +
            "WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Rating> findByNoteId(long noteId) throws SQLException {
        String sql =
            "SELECT r.*, u.username AS rater_username " +
            "FROM ratings r JOIN users u ON u.id = r.user_id " +
            "WHERE r.note_id = ? ORDER BY r.created_at DESC";
        List<Rating> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private Rating map(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getLong("id"));
        r.setNoteId(rs.getLong("note_id"));
        r.setUserId(rs.getLong("user_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setCreatedAt(ts.toLocalDateTime());
        r.setRaterUsername(rs.getString("rater_username"));
        return r;
    }
}
