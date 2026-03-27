package com.loosenotes.dao;

import com.loosenotes.model.Note;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteDao {

    public Optional<Note> findById(Connection conn, long id) throws SQLException {
        String sql = "SELECT n.*, u.username as owner_username FROM notes n JOIN users u ON n.user_id = u.id WHERE n.id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public long insert(Connection conn, Note note) throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, visibility) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, note.getUserId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setString(4, note.getVisibility() != null ? note.getVisibility() : "PRIVATE");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void update(Connection conn, Note note) throws SQLException {
        String sql = "UPDATE notes SET title = ?, content = ?, visibility = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getVisibility());
            ps.setLong(4, note.getId());
            ps.setLong(5, note.getUserId());
            ps.executeUpdate();
        }
    }

    public void delete(Connection conn, long noteId, long userId) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ? AND user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public void deleteById(Connection conn, long noteId) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        }
    }

    public List<Note> findByUserId(Connection conn, long userId, int page, int pageSize) throws SQLException {
        String sql = "SELECT n.*, u.username as owner_username FROM notes n JOIN users u ON n.user_id = u.id WHERE n.user_id = ? ORDER BY n.updated_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, pageSize);
            ps.setInt(3, (page - 1) * pageSize);
            return fetchList(ps);
        }
    }

    public long countByUserId(Connection conn, long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notes WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public List<Note> searchPublic(Connection conn, String query, int page, int pageSize) throws SQLException {
        String sql = "SELECT n.*, u.username as owner_username FROM notes n JOIN users u ON n.user_id = u.id WHERE n.visibility = 'PUBLIC' AND (LOWER(n.title) LIKE ? OR LOWER(n.content) LIKE ?) ORDER BY n.updated_at DESC LIMIT ? OFFSET ?";
        String like = "%" + query.toLowerCase() + "%";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setInt(3, pageSize);
            ps.setInt(4, (page - 1) * pageSize);
            return fetchList(ps);
        }
    }

    public List<Note> findTopRated(Connection conn, int limit) throws SQLException {
        String sql = "SELECT n.*, u.username as owner_username, AVG(r.rating) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n JOIN users u ON n.user_id = u.id LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.visibility = 'PUBLIC' GROUP BY n.id, u.username HAVING COUNT(r.id) > 0 ORDER BY avg_rating DESC, rating_count DESC LIMIT ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            List<Note> notes = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Note note = mapRow(rs);
                    note.setAverageRating(rs.getDouble("avg_rating"));
                    note.setRatingCount(rs.getInt("rating_count"));
                    notes.add(note);
                }
            }
            return notes;
        }
    }

    public List<Note> findAll(Connection conn, int page, int pageSize) throws SQLException {
        String sql = "SELECT n.*, u.username as owner_username FROM notes n JOIN users u ON n.user_id = u.id ORDER BY n.created_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, (page - 1) * pageSize);
            return fetchList(ps);
        }
    }

    public long countAll(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notes";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private List<Note> fetchList(PreparedStatement ps) throws SQLException {
        List<Note> notes = new ArrayList<>();
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) notes.add(mapRow(rs));
        }
        return notes;
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getLong("id"));
        n.setUserId(rs.getLong("user_id"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        n.setVisibility(rs.getString("visibility"));
        n.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        n.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        try { n.setOwnerUsername(rs.getString("owner_username")); } catch (SQLException ignored) {}
        return n;
    }
}
