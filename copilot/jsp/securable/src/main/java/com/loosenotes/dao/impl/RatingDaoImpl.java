package com.loosenotes.dao.impl;

import com.loosenotes.dao.RatingDao;
import com.loosenotes.model.Rating;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class RatingDaoImpl implements RatingDao {

    private static final Logger log = LoggerFactory.getLogger(RatingDaoImpl.class);
    private final DatabaseManager db;

    public RatingDaoImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public long insert(Rating r) {
        final String sql =
            "INSERT INTO ratings (note_id, user_id, stars, comment) VALUES (?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1,   r.getNoteId());
            ps.setLong(2,   r.getUserId());
            ps.setInt(3,    r.getStars());
            ps.setString(4, r.getComment());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            log.error("insert rating failed", e);
            throw new RuntimeException("Database error inserting rating", e);
        }
    }

    @Override
    public Optional<Rating> findById(long id) {
        return queryOne(
            "SELECT r.*, u.username AS rater FROM ratings r " +
            "JOIN users u ON r.user_id = u.id WHERE r.id=?",
            ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<Rating> findByNoteAndUser(long noteId, long userId) {
        return queryOne(
            "SELECT r.*, u.username AS rater FROM ratings r " +
            "JOIN users u ON r.user_id = u.id WHERE r.note_id=? AND r.user_id=?",
            ps -> { ps.setLong(1, noteId); ps.setLong(2, userId); });
    }

    @Override
    public List<Rating> findByNoteId(long noteId) {
        return queryMany(
            "SELECT r.*, u.username AS rater FROM ratings r " +
            "JOIN users u ON r.user_id = u.id WHERE r.note_id=? ORDER BY r.created_at DESC",
            ps -> ps.setLong(1, noteId));
    }

    @Override
    public boolean update(Rating r) {
        return executeUpdate(
            "UPDATE ratings SET stars=?, comment=?, updated_at=datetime('now') WHERE id=?",
            ps -> { ps.setInt(1, r.getStars()); ps.setString(2, r.getComment()); ps.setLong(3, r.getId()); });
    }

    @Override
    public double getAverageForNote(long noteId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT AVG(CAST(stars AS REAL)) FROM ratings WHERE note_id=?")) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble(1) : 0.0;
            }
        } catch (SQLException e) {
            log.error("getAverageForNote failed", e);
            return 0.0;
        }
    }

    @Override
    public int countForNote(long noteId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM ratings WHERE note_id=?")) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            log.error("countForNote failed", e);
            return 0;
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private Optional<Rating> queryOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("queryOne rating failed", e);
            return Optional.empty();
        }
    }

    private List<Rating> queryMany(String sql, StatementBinder binder) {
        List<Rating> results = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("queryMany ratings failed", e);
        }
        return results;
    }

    private boolean executeUpdate(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("executeUpdate rating failed", e);
            return false;
        }
    }

    private Rating mapRow(ResultSet rs) throws SQLException {
        return new Rating(
            rs.getLong("id"),
            rs.getLong("note_id"),
            rs.getLong("user_id"),
            rs.getInt("stars"),
            rs.getString("comment"),
            parseDateTime(rs.getString("created_at")),
            parseDateTime(rs.getString("updated_at")),
            rs.getString("rater")
        );
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null) return LocalDateTime.now();
        return LocalDateTime.parse(s.replace(" ", "T"));
    }
}
