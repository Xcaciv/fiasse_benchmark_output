package com.loosenotes.dao;

import com.loosenotes.model.Rating;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RatingDAO {
    private static final Logger LOGGER = Logger.getLogger(RatingDAO.class.getName());

    public Rating findByNoteAndUser(int noteId, int userId) {
        String sql = "SELECT r.*, u.username as rater_username FROM ratings r " +
                     "JOIN users u ON r.user_id = u.id " +
                     "WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding rating by note and user", e);
        }
        return null;
    }

    public List<Rating> findByNoteId(int noteId) {
        List<Rating> list = new ArrayList<>();
        String sql = "SELECT r.*, u.username as rater_username FROM ratings r " +
                     "JOIN users u ON r.user_id = u.id " +
                     "WHERE r.note_id = ? ORDER BY r.created_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding ratings for note", e);
        }
        return list;
    }

    public boolean create(Rating rating) {
        String sql = "INSERT INTO ratings (note_id, user_id, rating, comment) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, rating.getNoteId());
            ps.setInt(2, rating.getUserId());
            ps.setInt(3, rating.getRating());
            ps.setString(4, rating.getComment());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) rating.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating rating", e);
        }
        return false;
    }

    public boolean update(Rating rating) {
        String sql = "UPDATE ratings SET rating = ?, comment = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating.getRating());
            ps.setString(2, rating.getComment());
            ps.setInt(3, rating.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating rating", e);
        }
        return false;
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getInt("id"));
        r.setNoteId(rs.getInt("note_id"));
        r.setUserId(rs.getInt("user_id"));
        r.setRating(rs.getInt("rating"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getTimestamp("created_at"));
        r.setRaterUsername(rs.getString("rater_username"));
        return r;
    }
}
