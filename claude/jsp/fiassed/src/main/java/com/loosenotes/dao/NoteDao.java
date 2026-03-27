package com.loosenotes.dao;

import com.loosenotes.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code notes} table.
 * Visibility enum is persisted by name. All queries use PreparedStatements.
 * Rating aggregates (avg, count) are populated via JOIN queries where needed.
 */
public class NoteDao {

    private static final Logger log = LoggerFactory.getLogger(NoteDao.class);

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ------------------------------------------------------------------ reads

    public Note findById(Long id) {
        final String sql =
            "SELECT n.*, u.username, " +
            "AVG(r.rating_value) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.id = ? " +
            "GROUP BY n.id, u.username";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findById failed for id={}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    /**
     * Ownership-scoped lookup — returns null if the note does not belong to userId.
     */
    public Note findByIdForOwner(Long id, Long userId) {
        final String sql =
            "SELECT n.*, u.username, " +
            "AVG(r.rating_value) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.id = ? AND n.user_id = ? " +
            "GROUP BY n.id, u.username";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.setLong(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findByIdForOwner failed id={} userId={}: {}", id, userId, e.getMessage(), e);
            return null;
        }
    }

    public List<Note> findByUserId(Long userId, int page, int pageSize) {
        final String sql =
            "SELECT n.*, u.username, " +
            "AVG(r.rating_value) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.user_id = ? " +
            "GROUP BY n.id, u.username " +
            "ORDER BY n.updated_at DESC LIMIT ? OFFSET ?";
        List<Note> notes = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, pageSize);
            ps.setInt(3, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByUserId failed userId={}: {}", userId, e.getMessage(), e);
        }
        return notes;
    }

    /**
     * Returns notes owned by userId plus all PUBLIC notes, deduplicated.
     */
    public List<Note> findVisibleToUser(Long userId, int page, int pageSize) {
        final String sql =
            "SELECT n.*, u.username, " +
            "AVG(r.rating_value) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.visibility = 'PUBLIC' OR n.user_id = ? " +
            "GROUP BY n.id, u.username " +
            "ORDER BY n.updated_at DESC LIMIT ? OFFSET ?";
        List<Note> notes = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, pageSize);
            ps.setInt(3, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findVisibleToUser failed userId={}: {}", userId, e.getMessage(), e);
        }
        return notes;
    }

    /**
     * Full-text LIKE search over title and content, scoped to notes visible to userId.
     */
    public List<Note> searchVisibleToUser(Long userId, String query, int page, int pageSize) {
        final String sql =
            "SELECT n.*, u.username, " +
            "AVG(r.rating_value) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "WHERE (n.visibility = 'PUBLIC' OR n.user_id = ?) " +
            "  AND (n.title LIKE ? OR n.content LIKE ?) " +
            "GROUP BY n.id, u.username " +
            "ORDER BY n.updated_at DESC LIMIT ? OFFSET ?";
        List<Note> notes = new ArrayList<>();
        String pattern = "%" + query + "%";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setInt(4, pageSize);
            ps.setInt(5, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("searchVisibleToUser failed: {}", e.getMessage(), e);
        }
        return notes;
    }

    /**
     * Returns public notes with at least {@code minRatingCount} ratings, ordered by average
     * rating descending. Aggregation and filtering are performed entirely in SQL.
     */
    public List<Note> findTopRated(int minRatingCount, int pageSize) {
        final String sql =
            "SELECT n.*, u.username, " +
            "AVG(r.rating_value) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.visibility = 'PUBLIC' " +
            "GROUP BY n.id, u.username " +
            "HAVING COUNT(r.id) >= ? " +
            "ORDER BY avg_rating DESC, rating_count DESC " +
            "LIMIT ?";
        List<Note> notes = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, minRatingCount);
            ps.setInt(2, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findTopRated failed: {}", e.getMessage(), e);
        }
        return notes;
    }

    public int countByUserId(Long userId) {
        final String sql = "SELECT COUNT(*) FROM notes WHERE user_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            log.error("countByUserId failed userId={}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    // ----------------------------------------------------------------- writes

    public boolean create(Note note) {
        final String sql =
            "INSERT INTO notes (user_id, title, content, visibility) VALUES (?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, note.getUserId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setString(4, note.getVisibility().name());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) note.setId(keys.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("create note failed userId={}: {}", note.getUserId(), e.getMessage(), e);
        }
        return false;
    }

    public boolean update(Note note) {
        final String sql =
            "UPDATE notes SET title = ?, content = ?, visibility = ?, " +
            "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getVisibility().name());
            ps.setLong(4, note.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("update note failed id={}: {}", note.getId(), e.getMessage(), e);
            return false;
        }
    }

    public boolean delete(Long id) {
        final String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("delete note failed id={}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    /** Admin-only operation to transfer note ownership. */
    public boolean reassign(Long noteId, Long newUserId) {
        final String sql = "UPDATE notes SET user_id = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, newUserId);
            ps.setLong(2, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("reassign note failed noteId={} newUserId={}: {}", noteId, newUserId, e.getMessage(), e);
            return false;
        }
    }

    // --------------------------------------------------------------- helpers

    private Note mapRow(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setUsername(rs.getString("username"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        n.setVisibility(Note.Visibility.valueOf(rs.getString("visibility")));
        n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        n.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        double avg = rs.getDouble("avg_rating");
        if (!rs.wasNull()) n.setAverageRating(avg);
        int cnt = rs.getInt("rating_count");
        n.setRatingCount(cnt);
        return n;
    }
}
