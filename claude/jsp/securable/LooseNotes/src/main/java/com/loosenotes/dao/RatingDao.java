package com.loosenotes.dao;

import com.loosenotes.model.Rating;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Rating entities.
 * SSEM: Integrity - unique constraint enforced at DB + query level.
 * SSEM: Resilience - try-with-resources throughout.
 */
public class RatingDao {

    private final DataSource dataSource;

    public RatingDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Upserts a rating: creates if none exists for (noteId, userId), updates otherwise.
     */
    public void upsert(Rating rating) throws SQLException {
        Optional<Rating> existing = findByNoteAndUser(rating.getNoteId(), rating.getUserId());
        if (existing.isPresent()) {
            update(rating.getNoteId(), rating.getUserId(), rating.getValue(), rating.getComment());
        } else {
            create(rating);
        }
    }

    private long create(Rating rating) throws SQLException {
        String sql = "INSERT INTO ratings (note_id, user_id, value, comment) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rating.getNoteId());
            ps.setLong(2, rating.getUserId());
            ps.setInt(3, rating.getValue());
            ps.setString(4, rating.getComment());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Rating creation failed: no generated key");
    }

    private void update(long noteId, long userId, int value, String comment) throws SQLException {
        String sql = "UPDATE ratings SET value = ?, comment = ?, updated_at = CURRENT_TIMESTAMP "
            + "WHERE note_id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, value);
            ps.setString(2, comment);
            ps.setLong(3, noteId);
            ps.setLong(4, userId);
            ps.executeUpdate();
        }
    }

    /** Finds a rating by note and user (for detecting existing ratings). */
    public Optional<Rating> findByNoteAndUser(long noteId, long userId) throws SQLException {
        String sql = "SELECT r.*, u.username AS rater_username FROM ratings r "
            + "JOIN users u ON u.id = r.user_id WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Returns all ratings for a note, newest first. */
    public List<Rating> findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT r.*, u.username AS rater_username FROM ratings r "
            + "JOIN users u ON u.id = r.user_id WHERE r.note_id = ? "
            + "ORDER BY r.created_at DESC";
        List<Rating> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getLong("id"));
        r.setNoteId(rs.getLong("note_id"));
        r.setUserId(rs.getLong("user_id"));
        r.setRaterUsername(rs.getString("rater_username"));
        r.setValue(rs.getInt("value"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        r.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return r;
    }
}
