package com.loosenotes.dao.impl;

import com.loosenotes.dao.NoteDao;
import com.loosenotes.model.Note;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation — all queries use PreparedStatement (Integrity). */
public final class NoteDaoImpl implements NoteDao {

    private static final Logger log = LoggerFactory.getLogger(NoteDaoImpl.class);
    private final DatabaseManager db;

    public NoteDaoImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public long insert(Note note) {
        final String sql =
            "INSERT INTO notes (user_id, title, content, visibility) VALUES (?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, note.getUserId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setString(4, note.getVisibility().name());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            log.error("insert note failed", e);
            throw new RuntimeException("Database error inserting note", e);
        }
    }

    @Override
    public Optional<Note> findById(long id) {
        final String sql =
            "SELECT n.*, u.username AS author FROM notes n " +
            "JOIN users u ON n.user_id = u.id WHERE n.id=?";
        return queryOne(sql, ps -> ps.setLong(1, id));
    }

    @Override
    public List<Note> findByUserId(long userId) {
        final String sql =
            "SELECT n.*, u.username AS author FROM notes n " +
            "JOIN users u ON n.user_id = u.id WHERE n.user_id=? " +
            "ORDER BY n.updated_at DESC";
        return queryMany(sql, ps -> ps.setLong(1, userId));
    }

    @Override
    public List<Note> findPublicNotes() {
        final String sql =
            "SELECT n.*, u.username AS author FROM notes n " +
            "JOIN users u ON n.user_id = u.id WHERE n.visibility='PUBLIC' " +
            "ORDER BY n.updated_at DESC";
        return queryMany(sql, ps -> {});
    }

    /**
     * Search: returns owned notes (any visibility) + public notes from others.
     * Uses LIKE with parameterized values (Integrity: no concatenation).
     */
    @Override
    public List<Note> searchNotes(String keyword, long requestingUserId) {
        final String sql =
            "SELECT n.*, u.username AS author FROM notes n " +
            "JOIN users u ON n.user_id = u.id " +
            "WHERE (LOWER(n.title) LIKE LOWER(?) OR LOWER(n.content) LIKE LOWER(?)) " +
            "  AND (n.user_id=? OR n.visibility='PUBLIC') " +
            "ORDER BY n.updated_at DESC";
        String like = "%" + keyword + "%";
        return queryMany(sql, ps -> {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setLong(3, requestingUserId);
        });
    }

    @Override
    public List<Note> findTopRated(int minRatings, int limit) {
        final String sql =
            "SELECT n.*, u.username AS author, AVG(r.stars) AS avg_stars, COUNT(r.id) AS cnt " +
            "FROM notes n " +
            "JOIN users u ON n.user_id = u.id " +
            "JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.visibility='PUBLIC' " +
            "GROUP BY n.id HAVING cnt >= ? " +
            "ORDER BY avg_stars DESC LIMIT ?";
        return queryMany(sql, ps -> {
            ps.setInt(1, minRatings);
            ps.setInt(2, limit);
        });
    }

    @Override
    public boolean update(Note note) {
        final String sql =
            "UPDATE notes SET title=?, content=?, visibility=?, updated_at=datetime('now') " +
            "WHERE id=?";
        return executeUpdate(sql, ps -> {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getVisibility().name());
            ps.setLong(4, note.getId());
        });
    }

    @Override
    public boolean delete(long id) {
        return executeUpdate("DELETE FROM notes WHERE id=?", ps -> ps.setLong(1, id));
    }

    @Override
    public boolean changeOwner(long noteId, long newOwnerId) {
        return executeUpdate(
            "UPDATE notes SET user_id=?, updated_at=datetime('now') WHERE id=?", ps -> {
                ps.setLong(1, newOwnerId);
                ps.setLong(2, noteId);
            });
    }

    @Override
    public int countAll() {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM notes");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.error("countAll notes failed", e);
            return 0;
        }
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private Optional<Note> queryOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("queryOne note failed", e);
            return Optional.empty();
        }
    }

    private List<Note> queryMany(String sql, StatementBinder binder) {
        List<Note> results = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("queryMany notes failed", e);
        }
        return results;
    }

    private boolean executeUpdate(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("executeUpdate note failed", e);
            return false;
        }
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        String createdStr = rs.getString("created_at");
        String updatedStr = rs.getString("updated_at");
        LocalDateTime created = parseDateTime(createdStr);
        LocalDateTime updated = parseDateTime(updatedStr);
        Note.Visibility vis = Note.Visibility.valueOf(rs.getString("visibility"));
        return new Note(
            rs.getLong("id"),
            rs.getLong("user_id"),
            rs.getString("title"),
            rs.getString("content"),
            vis, created, updated,
            rs.getString("author")
        );
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null) return LocalDateTime.now();
        return LocalDateTime.parse(s.replace(" ", "T"));
    }
}
