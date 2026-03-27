package com.loosenotes.dao;

import com.loosenotes.model.Rating;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RatingDAO {

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getInt("id"));
        r.setNoteId(rs.getInt("note_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getString("created_at"));
        return r;
    }

    public Rating findByNoteAndUser(int noteId, int userId) {
        String sql = "SELECT * FROM ratings WHERE note_id = ? AND user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Rating> findByNoteId(int noteId) {
        String sql = "SELECT r.*, u.username FROM ratings r " +
                     "JOIN users u ON r.user_id = u.id " +
                     "WHERE r.note_id = ? ORDER BY r.created_at DESC";
        List<Rating> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Rating rating = mapRow(rs);
                    rating.setUsername(rs.getString("username"));
                    list.add(rating);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public double getAverageRating(int noteId) {
        String sql = "SELECT AVG(rating) FROM ratings WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0.0;
    }

    public int countByNoteId(int noteId) {
        String sql = "SELECT COUNT(*) FROM ratings WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    /** Inserts or replaces (upsert) a rating for the given note/user pair. */
    public void createOrUpdate(Rating rating) {
        String sql = "INSERT OR REPLACE INTO ratings (note_id, user_id, rating, comment, created_at) VALUES (?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating.getNoteId());
            ps.setInt(2, rating.getUserId());
            ps.setInt(3, rating.getRating());
            ps.setString(4, rating.getComment());
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteByNoteId(int noteId) {
        String sql = "DELETE FROM ratings WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
