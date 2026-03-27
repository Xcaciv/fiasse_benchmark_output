package com.loosenotes.dao;

import com.loosenotes.model.Note;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the notes table.
 * Search enforces visibility rules: owner sees all their notes; others see only public notes.
 */
public class NoteDao {

    private static final Logger log = LoggerFactory.getLogger(NoteDao.class);
    private final DatabaseManager dbManager;

    public NoteDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Note note) throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, is_public, created_at, updated_at) VALUES (?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, note.getUserId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setInt(4, note.isPublic() ? 1 : 0);
            ps.setLong(5, note.getCreatedAt());
            ps.setLong(6, note.getUpdatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    note.setId(keys.getLong(1));
                }
            }
        }
    }

    public Optional<Note> findById(long id) throws SQLException {
        String sql = "SELECT n.*, u.username AS owner_username FROM notes n "
                   + "LEFT JOIN users u ON n.user_id = u.id WHERE n.id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Note> findByUserId(long userId) throws SQLException {
        String sql = "SELECT n.*, u.username AS owner_username FROM notes n "
                   + "LEFT JOIN users u ON n.user_id = u.id WHERE n.user_id = ? ORDER BY n.updated_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    public void update(Note note) throws SQLException {
        String sql = "UPDATE notes SET title=?, content=?, is_public=?, updated_at=? WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setInt(3, note.isPublic() ? 1 : 0);
            ps.setLong(4, note.getUpdatedAt());
            ps.setLong(5, note.getId());
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /**
     * Search returning the current user's notes plus public notes from others.
     * Trust boundary: keyword is bound as a parameterised LIKE pattern — never concatenated.
     */
    public List<Note> searchNotes(long currentUserId, String keyword) throws SQLException {
        String sql = "SELECT n.*, u.username AS owner_username FROM notes n "
                   + "LEFT JOIN users u ON n.user_id = u.id "
                   + "WHERE (n.user_id = ? OR n.is_public = 1) "
                   + "AND (n.title LIKE ? OR n.content LIKE ?) "
                   + "ORDER BY n.updated_at DESC";
        String pattern = "%" + keyword + "%";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, currentUserId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    /** Returns public notes with at least minRatings ratings, sorted by average rating desc. */
    public List<Note> findPublicNotesSortedByRating(int minRatings) throws SQLException {
        String sql = "SELECT n.*, u.username AS owner_username, AVG(r.rating_value) AS avg_rating, "
                   + "COUNT(r.id) AS rating_count "
                   + "FROM notes n "
                   + "LEFT JOIN users u ON n.user_id = u.id "
                   + "JOIN ratings r ON r.note_id = n.id "
                   + "WHERE n.is_public = 1 "
                   + "GROUP BY n.id "
                   + "HAVING COUNT(r.id) >= ? "
                   + "ORDER BY avg_rating DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minRatings);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM notes";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    public void updateOwner(long noteId, long newUserId) throws SQLException {
        String sql = "UPDATE notes SET user_id=? WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, newUserId);
            ps.setLong(2, noteId);
            ps.executeUpdate();
        }
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getLong("id"));
        note.setUserId(rs.getLong("user_id"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        note.setPublic(rs.getInt("is_public") == 1);
        note.setCreatedAt(rs.getLong("created_at"));
        note.setUpdatedAt(rs.getLong("updated_at"));
        try {
            note.setOwnerUsername(rs.getString("owner_username"));
        } catch (SQLException ignored) {
            // owner_username column not always present
        }
        return note;
    }

    private List<Note> mapRows(ResultSet rs) throws SQLException {
        List<Note> notes = new ArrayList<>();
        while (rs.next()) {
            notes.add(mapRow(rs));
        }
        return notes;
    }
}
