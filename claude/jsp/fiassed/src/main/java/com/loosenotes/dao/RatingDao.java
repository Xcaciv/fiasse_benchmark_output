package com.loosenotes.dao;

import com.loosenotes.model.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code ratings} table.
 * The DB unique constraint (note_id, user_id) enforces one rating per user per note;
 * the DAO exposes both create and update to allow amendment.
 */
public class RatingDao {

    private static final Logger log = LoggerFactory.getLogger(RatingDao.class);

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ------------------------------------------------------------------ reads

    public Rating findById(Long id) {
        final String sql =
            "SELECT r.*, u.username FROM ratings r " +
            "JOIN users u ON u.id = r.user_id WHERE r.id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findById failed id={}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    public Rating findByNoteAndUser(Long noteId, Long userId) {
        final String sql =
            "SELECT r.*, u.username FROM ratings r " +
            "JOIN users u ON u.id = r.user_id " +
            "WHERE r.note_id = ? AND r.user_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findByNoteAndUser failed noteId={} userId={}: {}", noteId, userId, e.getMessage(), e);
            return null;
        }
    }

    public List<Rating> findByNoteId(Long noteId, int page, int pageSize) {
        final String sql =
            "SELECT r.*, u.username FROM ratings r " +
            "JOIN users u ON u.id = r.user_id " +
            "WHERE r.note_id = ? " +
            "ORDER BY r.created_at DESC LIMIT ? OFFSET ?";
        List<Rating> list = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setInt(2, pageSize);
            ps.setInt(3, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByNoteId failed noteId={}: {}", noteId, e.getMessage(), e);
        }
        return list;
    }

    public int countByNoteId(Long noteId) {
        final String sql = "SELECT COUNT(*) FROM ratings WHERE note_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            log.error("countByNoteId failed noteId={}: {}", noteId, e.getMessage(), e);
            return 0;
        }
    }

    // ----------------------------------------------------------------- writes

    public boolean create(Rating rating) {
        final String sql =
            "INSERT INTO ratings (note_id, user_id, rating_value, comment) VALUES (?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, rating.getNoteId());
            ps.setLong(2, rating.getUserId());
            ps.setInt(3, rating.getRatingValue());
            ps.setString(4, rating.getComment());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) rating.setId(keys.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("create rating failed noteId={} userId={}: {}", rating.getNoteId(), rating.getUserId(), e.getMessage(), e);
        }
        return false;
    }

    /** Updates rating_value and comment only; note_id and user_id are immutable after creation. */
    public boolean update(Rating rating) {
        final String sql =
            "UPDATE ratings SET rating_value = ?, comment = ?, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, rating.getRatingValue());
            ps.setString(2, rating.getComment());
            ps.setLong(3, rating.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("update rating failed id={}: {}", rating.getId(), e.getMessage(), e);
            return false;
        }
    }

    public boolean delete(Long id) {
        final String sql = "DELETE FROM ratings WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("delete rating failed id={}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    // --------------------------------------------------------------- helpers

    private Rating mapRow(ResultSet rs) throws SQLException {
        Rating r = new Rating();
        r.setId(rs.getLong("id"));
        r.setNoteId(rs.getLong("note_id"));
        r.setUserId(rs.getLong("user_id"));
        r.setUsername(rs.getString("username"));
        r.setRatingValue(rs.getInt("rating_value"));
        r.setComment(rs.getString("comment"));
        r.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updated = rs.getTimestamp("updated_at");
        if (updated != null) r.setUpdatedAt(updated.toLocalDateTime());
        return r;
    }
}
