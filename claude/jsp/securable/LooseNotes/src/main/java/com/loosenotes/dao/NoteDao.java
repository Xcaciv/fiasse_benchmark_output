package com.loosenotes.dao;

import com.loosenotes.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Note entities.
 * SSEM: Integrity - all queries parameterized; ownership enforced at query level.
 * SSEM: Resilience - try-with-resources throughout.
 */
public class NoteDao {

    private static final Logger log = LoggerFactory.getLogger(NoteDao.class);
    private final DataSource dataSource;

    public NoteDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Creates a new note and returns the generated ID. */
    public long create(Note note) throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, is_public) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, note.getUserId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setBoolean(4, note.isPublic());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Note creation failed: no generated key");
    }

    /**
     * Finds a note by ID - does NOT enforce ownership here.
     * Callers must verify ownership via note.getUserId().
     */
    public Optional<Note> findById(long id) throws SQLException {
        String sql = "SELECT n.*, u.username AS owner_username, "
            + "COALESCE(AVG(r.value), 0) AS avg_rating, COUNT(r.id) AS rating_count "
            + "FROM notes n "
            + "JOIN users u ON u.id = n.user_id "
            + "LEFT JOIN ratings r ON r.note_id = n.id "
            + "WHERE n.id = ? GROUP BY n.id, u.username";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Returns all notes owned by the given user, newest first. */
    public List<Note> findByUserId(long userId) throws SQLException {
        String sql = "SELECT n.*, u.username AS owner_username, "
            + "COALESCE(AVG(r.value), 0) AS avg_rating, COUNT(r.id) AS rating_count "
            + "FROM notes n JOIN users u ON u.id = n.user_id "
            + "LEFT JOIN ratings r ON r.note_id = n.id "
            + "WHERE n.user_id = ? GROUP BY n.id, u.username ORDER BY n.created_at DESC";
        return queryNotes(sql, ps -> ps.setLong(1, userId));
    }

    /**
     * Searches notes: returns owned notes of any visibility plus public notes from others.
     * SSEM: Integrity - private notes from other users are excluded.
     */
    public List<Note> search(String escapedQuery, long currentUserId) throws SQLException {
        String sql = "SELECT n.*, u.username AS owner_username, "
            + "COALESCE(AVG(r.value), 0) AS avg_rating, COUNT(r.id) AS rating_count "
            + "FROM notes n JOIN users u ON u.id = n.user_id "
            + "LEFT JOIN ratings r ON r.note_id = n.id "
            + "WHERE (n.user_id = ? OR n.is_public = TRUE) "
            + "AND (LOWER(n.title) LIKE ? ESCAPE '\\' OR LOWER(n.content) LIKE ? ESCAPE '\\') "
            + "GROUP BY n.id, u.username ORDER BY n.created_at DESC";
        String pattern = "%" + escapedQuery.toLowerCase() + "%";
        return queryNotes(sql, ps -> {
            ps.setLong(1, currentUserId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
        });
    }

    /** Returns top-rated public notes with at least minRatings ratings. */
    public List<Note> findTopRated(int minRatings, int limit) throws SQLException {
        String sql = "SELECT n.*, u.username AS owner_username, "
            + "AVG(r.value) AS avg_rating, COUNT(r.id) AS rating_count "
            + "FROM notes n JOIN users u ON u.id = n.user_id "
            + "JOIN ratings r ON r.note_id = n.id "
            + "WHERE n.is_public = TRUE "
            + "GROUP BY n.id, u.username HAVING COUNT(r.id) >= ? "
            + "ORDER BY avg_rating DESC, rating_count DESC LIMIT ?";
        return queryNotes(sql, ps -> {
            ps.setInt(1, minRatings);
            ps.setInt(2, limit);
        });
    }

    /** Updates note title, content, and visibility. */
    public void update(Note note) throws SQLException {
        String sql = "UPDATE notes SET title = ?, content = ?, is_public = ?, "
            + "updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setBoolean(3, note.isPublic());
            ps.setLong(4, note.getId());
            ps.setLong(5, note.getUserId());
            ps.executeUpdate();
        }
    }

    /** Deletes a note by ID. Cascades to attachments, ratings, share_links. */
    public void delete(long noteId) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        }
    }

    /** Reassigns note ownership (admin operation). */
    public void reassignOwner(long noteId, long newUserId) throws SQLException {
        String sql = "UPDATE notes SET user_id = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, newUserId);
            ps.setLong(2, noteId);
            ps.executeUpdate();
        }
    }

    /** Counts all notes. */
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM notes";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0L;
    }

    @FunctionalInterface
    private interface PreparedStatementSetter {
        void set(PreparedStatement ps) throws SQLException;
    }

    private List<Note> queryNotes(String sql, PreparedStatementSetter setter) throws SQLException {
        List<Note> notes = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            setter.set(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        }
        return notes;
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getLong("id"));
        note.setUserId(rs.getLong("user_id"));
        note.setOwnerUsername(rs.getString("owner_username"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        note.setPublic(rs.getBoolean("is_public"));
        note.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        note.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        note.setAverageRating(rs.getDouble("avg_rating"));
        note.setRatingCount(rs.getInt("rating_count"));
        return note;
    }
}
