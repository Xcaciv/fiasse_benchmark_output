package com.loosenotes.dao;

import com.loosenotes.model.Note;
import com.loosenotes.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NoteDAO {

    // ── Create ────────────────────────────────────────────────────────────

    public long create(long userId, String title, String content, boolean isPublic)
            throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, is_public) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setBoolean(4, isPublic);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    // ── Read ──────────────────────────────────────────────────────────────

    public Note findById(long id) throws SQLException {
        String sql =
            "SELECT n.*, u.username AS author_username, " +
            "  AVG(r.rating) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.id = ? " +
            "GROUP BY n.id, u.username";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Notes visible to the given viewer: owned notes + public notes. */
    public List<Note> findVisibleTo(long viewerId) throws SQLException {
        String sql =
            "SELECT n.*, u.username AS author_username, " +
            "  AVG(r.rating) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.user_id = ? OR n.is_public = TRUE " +
            "GROUP BY n.id, u.username " +
            "ORDER BY n.updated_at DESC";
        return query(sql, viewerId);
    }

    /** Notes owned by a specific user. */
    public List<Note> findByUserId(long userId) throws SQLException {
        String sql =
            "SELECT n.*, u.username AS author_username, " +
            "  AVG(r.rating) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.user_id = ? " +
            "GROUP BY n.id, u.username " +
            "ORDER BY n.updated_at DESC";
        return query(sql, userId);
    }

    /**
     * Search: returns owned notes (any visibility) + public notes from others
     * matching the keyword in title or content (case-insensitive).
     */
    public List<Note> search(long viewerId, String keyword) throws SQLException {
        String sql =
            "SELECT n.*, u.username AS author_username, " +
            "  AVG(r.rating) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "WHERE (n.user_id = ? OR n.is_public = TRUE) " +
            "  AND (LOWER(n.title) LIKE ? OR LOWER(n.content) LIKE ?) " +
            "GROUP BY n.id, u.username " +
            "ORDER BY n.updated_at DESC";
        String like = "%" + keyword.toLowerCase() + "%";
        List<Note> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, viewerId);
            ps.setString(2, like);
            ps.setString(3, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Top-rated public notes with at least 3 ratings, sorted by average rating desc.
     */
    public List<Note> findTopRated() throws SQLException {
        String sql =
            "SELECT n.*, u.username AS author_username, " +
            "  AVG(r.rating) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "JOIN ratings r ON r.note_id = n.id " +
            "WHERE n.is_public = TRUE " +
            "GROUP BY n.id, u.username " +
            "HAVING COUNT(r.id) >= 3 " +
            "ORDER BY avg_rating DESC, rating_count DESC";
        List<Note> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public long countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM notes";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0;
        }
    }

    public List<Note> findRecent(int limit) throws SQLException {
        String sql =
            "SELECT n.*, u.username AS author_username, " +
            "  AVG(r.rating) AS avg_rating, COUNT(r.id) AS rating_count " +
            "FROM notes n " +
            "JOIN users u ON u.id = n.user_id " +
            "LEFT JOIN ratings r ON r.note_id = n.id " +
            "GROUP BY n.id, u.username " +
            "ORDER BY n.created_at DESC LIMIT ?";
        List<Note> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    // ── Update ────────────────────────────────────────────────────────────

    public void update(long id, String title, String content, boolean isPublic)
            throws SQLException {
        String sql = "UPDATE notes SET title = ?, content = ?, is_public = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setBoolean(3, isPublic);
            ps.setLong(4, id);
            ps.executeUpdate();
        }
    }

    public void reassignOwner(long noteId, long newOwnerId) throws SQLException {
        String sql = "UPDATE notes SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, newOwnerId);
            ps.setLong(2, noteId);
            ps.executeUpdate();
        }
    }

    // ── Delete ────────────────────────────────────────────────────────────

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private List<Note> query(String sql, long param) throws SQLException {
        List<Note> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    private Note map(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        n.setPublic(rs.getBoolean("is_public"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) n.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) n.setUpdatedAt(ua.toLocalDateTime());
        n.setAuthorUsername(rs.getString("author_username"));
        double avg = rs.getDouble("avg_rating");
        n.setAverageRating(rs.wasNull() ? 0.0 : avg);
        n.setRatingCount(rs.getInt("rating_count"));
        return n;
    }
}
