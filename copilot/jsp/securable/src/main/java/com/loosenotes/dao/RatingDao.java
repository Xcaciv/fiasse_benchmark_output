package com.loosenotes.dao;

import com.loosenotes.model.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the ratings table.
 * Joins with users to provide display usernames without exposing PII.
 */
public class RatingDao {

    private static final Logger log = LoggerFactory.getLogger(RatingDao.class);
    private final DatabaseManager dbManager;

    public RatingDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Rating rating) throws SQLException {
        String sql = "INSERT INTO ratings (note_id, user_id, rating_value, comment, created_at, updated_at) VALUES (?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rating.getNoteId());
            ps.setLong(2, rating.getUserId());
            ps.setInt(3, rating.getRatingValue());
            ps.setString(4, rating.getComment());
            ps.setLong(5, rating.getCreatedAt());
            ps.setLong(6, rating.getUpdatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    rating.setId(keys.getLong(1));
                }
            }
        }
    }

    public Optional<Rating> findByNoteAndUser(long noteId, long userId) throws SQLException {
        String sql = "SELECT r.*, u.username FROM ratings r "
                   + "JOIN users u ON r.user_id = u.id "
                   + "WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Rating> findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT r.*, u.username FROM ratings r "
                   + "JOIN users u ON r.user_id = u.id "
                   + "WHERE r.note_id = ? ORDER BY r.created_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    public double getAverageRating(long noteId) throws SQLException {
        String sql = "SELECT AVG(rating_value) FROM ratings WHERE note_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    double avg = rs.getDouble(1);
                    return rs.wasNull() ? 0.0 : avg;
                }
                return 0.0;
            }
        }
    }

    public void update(Rating rating) throws SQLException {
        String sql = "UPDATE ratings SET rating_value=?, comment=?, updated_at=? WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating.getRatingValue());
            ps.setString(2, rating.getComment());
            ps.setLong(3, rating.getUpdatedAt());
            ps.setLong(4, rating.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM ratings WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void deleteByNoteId(long noteId) throws SQLException {
        String sql = "DELETE FROM ratings WHERE note_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        }
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getLong("id"));
        r.setNoteId(rs.getLong("note_id"));
        r.setUserId(rs.getLong("user_id"));
        r.setRatingValue(rs.getInt("rating_value"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getLong("created_at"));
        r.setUpdatedAt(rs.getLong("updated_at"));
        r.setUsername(rs.getString("username"));
        return r;
    }

    private List<Rating> mapRows(ResultSet rs) throws SQLException {
        List<Rating> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
