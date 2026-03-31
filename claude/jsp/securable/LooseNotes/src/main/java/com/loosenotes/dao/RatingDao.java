package com.loosenotes.dao;

import com.loosenotes.model.Rating;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the ratings table.
 *
 * SSEM notes:
 * - Integrity: upsert logic (insert or update) ensures one rating per user per note.
 * - Analyzability: separate findByNoteAndUser method supports ownership check in service.
 */
public class RatingDao {

    private final DatabaseManager db;

    public RatingDao(DatabaseManager db) {
        this.db = db;
    }

    /** Returns the rating for a specific user + note combination. */
    public Optional<Rating> findByNoteAndUser(long noteId, long userId) throws SQLException {
        String sql = "SELECT r.id, r.note_id, r.user_id, u.username AS rater_username, "
                + "r.stars, r.comment, r.created_at, r.updated_at "
                + "FROM ratings r JOIN users u ON u.id = r.user_id "
                + "WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Returns all ratings for a note, newest first. */
    public List<Rating> findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT r.id, r.note_id, r.user_id, u.username AS rater_username, "
                + "r.stars, r.comment, r.created_at, r.updated_at "
                + "FROM ratings r JOIN users u ON u.id = r.user_id "
                + "WHERE r.note_id = ? ORDER BY r.created_at DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Rating> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    /**
     * Inserts a new rating. Throws if the user has already rated this note
     * (UNIQUE constraint on note_id, user_id).
     */
    public long insert(Rating rating) throws SQLException {
        String sql = "INSERT INTO ratings (note_id, user_id, stars, comment) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rating.getNoteId());
            ps.setLong(2, rating.getUserId());
            ps.setInt(3, rating.getStars());
            ps.setString(4, rating.getComment());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No generated key for rating insert");
            }
        }
    }

    /** Updates an existing rating (user editing their own rating). */
    public void update(Rating rating) throws SQLException {
        String sql = "UPDATE ratings SET stars = ?, comment = ?, "
                + "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, rating.getStars());
            ps.setString(2, rating.getComment());
            ps.setLong(3, rating.getId());
            ps.executeUpdate();
        }
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getLong("id"));
        r.setNoteId(rs.getLong("note_id"));
        r.setUserId(rs.getLong("user_id"));
        r.setRaterUsername(rs.getString("rater_username"));
        r.setStars(rs.getInt("stars"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        r.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return r;
    }
}
