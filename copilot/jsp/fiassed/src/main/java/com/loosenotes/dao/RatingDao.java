package com.loosenotes.dao;

import com.loosenotes.model.Rating;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RatingDao {

    public long upsert(Connection conn, Rating r) throws SQLException {
        Optional<Rating> existing = findByNoteAndUser(conn, r.getNoteId(), r.getUserId());
        if (existing.isPresent()) {
            String sql = "UPDATE ratings SET rating = ?, comment = ?, updated_at = CURRENT_TIMESTAMP WHERE note_id = ? AND user_id = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, r.getRating());
                ps.setString(2, r.getComment());
                ps.setLong(3, r.getNoteId());
                ps.setLong(4, r.getUserId());
                ps.executeUpdate();
                return existing.get().getId();
            }
        } else {
            String sql = "INSERT INTO ratings (note_id, user_id, rating, comment) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, r.getNoteId());
                ps.setLong(2, r.getUserId());
                ps.setInt(3, r.getRating());
                ps.setString(4, r.getComment());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    rs.next();
                    return rs.getLong(1);
                }
            }
        }
    }

    public Optional<Rating> findByNoteAndUser(Connection conn, long noteId, long userId) throws SQLException {
        String sql = "SELECT r.*, u.username as rater_username FROM ratings r JOIN users u ON r.user_id = u.id WHERE r.note_id = ? AND r.user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Rating> findByNoteId(Connection conn, long noteId) throws SQLException {
        String sql = "SELECT r.*, u.username as rater_username FROM ratings r JOIN users u ON r.user_id = u.id WHERE r.note_id = ? ORDER BY r.created_at DESC";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            List<Rating> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        }
    }

    public double getAverageRating(Connection conn, long noteId) throws SQLException {
        String sql = "SELECT AVG(rating) FROM ratings WHERE note_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
                return 0.0;
            }
        }
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getLong("id"));
        r.setNoteId(rs.getLong("note_id"));
        r.setUserId(rs.getLong("user_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        r.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        try { r.setRaterUsername(rs.getString("rater_username")); } catch (SQLException ignored) {}
        return r;
    }
}
