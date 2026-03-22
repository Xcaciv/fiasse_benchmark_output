package com.loosenotes.dao;

import com.loosenotes.model.Note;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NoteDAO {

    public int createNote(int userId, String title, String content, boolean isPublic) throws Exception {
        String sql = "INSERT INTO notes (user_id, title, content, is_public, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String now = LocalDateTime.now().toString();
            ps.setInt(1, userId);
            ps.setString(2, title);
            ps.setString(3, content);
            ps.setInt(4, isPublic ? 1 : 0);
            ps.setString(5, now);
            ps.setString(6, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getInt(1);
            }
        }
        return -1;
    }

    public Note getNoteById(int id) throws Exception {
        String sql = "SELECT n.*, u.username FROM notes n JOIN users u ON n.user_id = u.id WHERE n.id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapNote(rs);
            }
        }
        return null;
    }

    public List<Note> getNotesByUser(int userId) throws Exception {
        String sql = "SELECT n.*, u.username FROM notes n JOIN users u ON n.user_id = u.id WHERE n.user_id = ? ORDER BY n.updated_at DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapNote(rs));
            }
        }
        return notes;
    }

    public void updateNote(int id, String title, String content, boolean isPublic) throws Exception {
        String sql = "UPDATE notes SET title = ?, content = ?, is_public = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setString(2, content);
            ps.setInt(3, isPublic ? 1 : 0);
            ps.setString(4, LocalDateTime.now().toString());
            ps.setInt(5, id);
            ps.executeUpdate();
        }
    }

    public void deleteNote(int id) throws Exception {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public List<Note> searchNotes(String keyword, int currentUserId) throws Exception {
        String sql = "SELECT n.*, u.username FROM notes n JOIN users u ON n.user_id = u.id " +
                     "WHERE (n.user_id = ? OR n.is_public = 1) " +
                     "AND (LOWER(n.title) LIKE ? OR LOWER(n.content) LIKE ?) " +
                     "ORDER BY n.updated_at DESC";
        List<Note> notes = new ArrayList<>();
        String kw = "%" + keyword.toLowerCase() + "%";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, currentUserId);
            ps.setString(2, kw);
            ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapNote(rs));
            }
        }
        return notes;
    }

    public List<Note> getPublicNotesSortedByRating(int minRatings) throws Exception {
        String sql = "SELECT n.*, u.username, " +
                     "AVG(r.rating) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.is_public = 1 " +
                     "GROUP BY n.id " +
                     "HAVING COUNT(r.id) >= ? " +
                     "ORDER BY avg_rating DESC, rating_count DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minRatings);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Note note = mapNote(rs);
                    note.setAvgRating(rs.getDouble("avg_rating"));
                    note.setRatingCount(rs.getInt("rating_count"));
                    notes.add(note);
                }
            }
        }
        return notes;
    }

    public int getNoteCount() throws Exception {
        String sql = "SELECT COUNT(*) FROM notes";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getInt(1);
        }
        return 0;
    }

    public List<Note> getAllNotes() throws Exception {
        String sql = "SELECT n.*, u.username FROM notes n JOIN users u ON n.user_id = u.id ORDER BY n.updated_at DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) notes.add(mapNote(rs));
        }
        return notes;
    }

    public void reassignNote(int noteId, int newUserId) throws Exception {
        String sql = "UPDATE notes SET user_id = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newUserId);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setInt(3, noteId);
            ps.executeUpdate();
        }
    }

    private Note mapNote(ResultSet rs) throws SQLException {
        Note n = new Note();
        n.setId(rs.getInt("id"));
        n.setUserId(rs.getInt("user_id"));
        n.setTitle(rs.getString("title"));
        n.setContent(rs.getString("content"));
        n.setPublic(rs.getInt("is_public") == 1);
        n.setCreatedAt(rs.getString("created_at"));
        n.setUpdatedAt(rs.getString("updated_at"));
        n.setUsername(rs.getString("username"));
        return n;
    }
}
