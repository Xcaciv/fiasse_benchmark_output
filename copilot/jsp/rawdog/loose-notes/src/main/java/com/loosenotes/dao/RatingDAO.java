package com.loosenotes.dao;

import com.loosenotes.model.Rating;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RatingDAO {

    public void addOrUpdateRating(int noteId, int userId, int rating, String comment) throws Exception {
        String sql = "INSERT INTO ratings (note_id, user_id, rating, comment, created_at) VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT(note_id, user_id) DO UPDATE SET rating = excluded.rating, comment = excluded.comment, created_at = excluded.created_at";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            ps.setInt(3, rating);
            ps.setString(4, comment);
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    public List<Rating> getRatingsByNote(int noteId) throws Exception {
        String sql = "SELECT r.*, u.username FROM ratings r JOIN users u ON r.user_id = u.id WHERE r.note_id = ? ORDER BY r.created_at DESC";
        List<Rating> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRating(rs));
            }
        }
        return list;
    }

    public Rating getRatingByNoteAndUser(int noteId, int userId) throws Exception {
        String sql = "SELECT r.*, u.username FROM ratings r JOIN users u ON r.user_id = u.id WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRating(rs);
            }
        }
        return null;
    }

    public double getAverageRating(int noteId) throws Exception {
        String sql = "SELECT AVG(rating) FROM ratings WHERE note_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        }
        return 0.0;
    }

    public int getRatingCount(int noteId) throws Exception {
        String sql = "SELECT COUNT(*) FROM ratings WHERE note_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        }
        return 0;
    }

    public void deleteRatingsByNote(int noteId) throws Exception {
        String sql = "DELETE FROM ratings WHERE note_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
        }
    }

    private Rating mapRating(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getInt("id"));
        r.setNoteId(rs.getInt("note_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getString("created_at"));
        r.setUsername(rs.getString("username"));
        return r;
    }
}
