package com.loosenotes.dao;

import com.loosenotes.model.Note;
import com.loosenotes.model.Note.Visibility;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the notes table.
 *
 * SSEM notes:
 * - Integrity: all queries use PreparedStatement; visibility is mapped via enum.
 * - Confidentiality: search only returns owned + public notes – enforced at SQL level.
 * - Analyzability: each method ≤ 30 LoC with single responsibility.
 */
public class NoteDao {

    private final DatabaseManager db;

    public NoteDao(DatabaseManager db) {
        this.db = db;
    }

    /** Returns a note by ID, joining the author's username. */
    public Optional<Note> findById(long id) throws SQLException {
        String sql = "SELECT n.id, n.user_id, u.username AS author_username, n.title, "
                + "n.content, n.visibility, n.created_at, n.updated_at, "
                + "COALESCE(AVG(r.stars), 0) AS avg_rating, COUNT(r.id) AS rating_count "
                + "FROM notes n JOIN users u ON u.id = n.user_id "
                + "LEFT JOIN ratings r ON r.note_id = n.id "
                + "WHERE n.id = ? GROUP BY n.id, u.username";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Returns all notes owned by a user (any visibility). */
    public List<Note> findByUserId(long userId) throws SQLException {
        String sql = "SELECT n.id, n.user_id, u.username AS author_username, n.title, "
                + "n.content, n.visibility, n.created_at, n.updated_at, "
                + "COALESCE(AVG(r.stars), 0) AS avg_rating, COUNT(r.id) AS rating_count "
                + "FROM notes n JOIN users u ON u.id = n.user_id "
                + "LEFT JOIN ratings r ON r.note_id = n.id "
                + "WHERE n.user_id = ? GROUP BY n.id, u.username ORDER BY n.updated_at DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    /**
     * Searches notes visible to a given user:
     * - All notes owned by the user (any visibility)
     * - Public notes owned by others
     * Match is case-insensitive on title or content.
     */
    public List<Note> search(String query, long requestingUserId) throws SQLException {
        String sql = "SELECT n.id, n.user_id, u.username AS author_username, n.title, "
                + "n.content, n.visibility, n.created_at, n.updated_at, "
                + "COALESCE(AVG(r.stars), 0) AS avg_rating, COUNT(r.id) AS rating_count "
                + "FROM notes n JOIN users u ON u.id = n.user_id "
                + "LEFT JOIN ratings r ON r.note_id = n.id "
                + "WHERE (n.user_id = ? OR n.visibility = 'PUBLIC') "
                + "AND (LOWER(n.title) LIKE ? OR LOWER(n.content) LIKE ?) "
                + "GROUP BY n.id, u.username ORDER BY n.updated_at DESC";
        String pattern = "%" + query.toLowerCase() + "%";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, requestingUserId);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    /**
     * Returns top-rated public notes with at least {@code minRatings} ratings,
     * sorted by average rating descending.
     */
    public List<Note> findTopRated(int minRatings, int limit) throws SQLException {
        String sql = "SELECT n.id, n.user_id, u.username AS author_username, n.title, "
                + "n.content, n.visibility, n.created_at, n.updated_at, "
                + "AVG(r.stars) AS avg_rating, COUNT(r.id) AS rating_count "
                + "FROM notes n JOIN users u ON u.id = n.user_id "
                + "JOIN ratings r ON r.note_id = n.id "
                + "WHERE n.visibility = 'PUBLIC' "
                + "GROUP BY n.id, u.username HAVING COUNT(r.id) >= ? "
                + "ORDER BY avg_rating DESC LIMIT ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minRatings);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    /** Inserts a new note. Returns the generated ID. */
    public long insert(Note note) throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, visibility) VALUES (?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, note.getUserId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setString(4, note.getVisibility().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No generated key for note insert");
            }
        }
    }

    /** Updates title, content, and visibility of an existing note. */
    public void update(Note note) throws SQLException {
        String sql = "UPDATE notes SET title = ?, content = ?, visibility = ?, "
                + "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getVisibility().name());
            ps.setLong(4, note.getId());
            ps.executeUpdate();
        }
    }

    /** Deletes a note by ID. Cascades to attachments, ratings, share_links. */
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    /** Updates the owner of a note (admin reassignment). */
    public void reassignOwner(long noteId, long newUserId) throws SQLException {
        String sql = "UPDATE notes SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, newUserId);
            ps.setLong(2, noteId);
            ps.executeUpdate();
        }
    }

    /** Returns total note count (admin dashboard). */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM notes";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setAuthorUsername(rs.getString("author_username"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        n.setVisibility(Visibility.valueOf(rs.getString("visibility")));
        n.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        n.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        n.setAverageRating(rs.getDouble("avg_rating"));
        n.setRatingCount(rs.getInt("rating_count"));
        return n;
    }

    private List<Note> mapRows(ResultSet rs) throws SQLException {
        List<Note> notes = new ArrayList<>();
        while (rs.next()) notes.add(mapRow(rs));
        return notes;
    }
}
