package com.loosenotes.dao;

import com.loosenotes.model.Rating;
import com.loosenotes.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class RatingDAO {

    public Rating findById(int id) throws SQLException {
        String sql = "SELECT r.id, r.note_id, r.user_id, r.rating, r.comment, r.created_at, r.updated_at, " +
                     "u.username FROM ratings r JOIN users u ON r.user_id = u.id WHERE r.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public Rating findByNoteAndUser(int noteId, int userId) throws SQLException {
        String sql = "SELECT r.id, r.note_id, r.user_id, r.rating, r.comment, r.created_at, r.updated_at, " +
                     "u.username FROM ratings r JOIN users u ON r.user_id = u.id " +
                     "WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.setInt(2, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Rating> findByNoteId(int noteId) throws SQLException {
        String sql = "SELECT r.id, r.note_id, r.user_id, r.rating, r.comment, r.created_at, r.updated_at, " +
                     "u.username FROM ratings r JOIN users u ON r.user_id = u.id " +
                     "WHERE r.note_id = ? ORDER BY r.created_at DESC";
        List<Rating> ratings = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    ratings.add(mapRow(rs));
                }
            }
        }
        return ratings;
    }

    public double getAverageRating(int noteId) throws SQLException {
        String sql = "SELECT AVG(rating) FROM ratings WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble(1);
                }
            }
        }
        return 0.0;
    }

    public int getRatingCount(int noteId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ratings WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public int create(int noteId, int userId, int rating, String comment) throws SQLException {
        String sql = "INSERT INTO ratings (note_id, user_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, noteId);
            pstmt.setInt(2, userId);
            pstmt.setInt(3, rating);
            pstmt.setString(4, comment);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(int id, int rating, String comment) throws SQLException {
        String sql = "UPDATE ratings SET rating = ?, comment = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, rating);
            pstmt.setString(2, comment);
            pstmt.setInt(3, id);
            pstmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM ratings WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating rating = new Rating();
        rating.setId(rs.getInt("id"));
        rating.setNoteId(rs.getInt("note_id"));
        rating.setUserId(rs.getInt("user_id"));
        rating.setRating(rs.getInt("rating"));
        rating.setComment(rs.getString("comment"));
        rating.setUsername(rs.getString("username"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            try {
                rating.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
            } catch (Exception e) {
                rating.setCreatedAt(LocalDateTime.now());
            }
        }
        String updatedAt = rs.getString("updated_at");
        if (updatedAt != null) {
            try {
                rating.setUpdatedAt(LocalDateTime.parse(updatedAt.replace(" ", "T")));
            } catch (Exception e) {
                rating.setUpdatedAt(LocalDateTime.now());
            }
        }
        return rating;
    }
}
