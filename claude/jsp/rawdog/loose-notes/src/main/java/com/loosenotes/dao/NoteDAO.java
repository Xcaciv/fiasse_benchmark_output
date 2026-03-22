package com.loosenotes.dao;

import com.loosenotes.model.Note;
import com.loosenotes.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NoteDAO {

    public Note findById(int id) throws SQLException {
        String sql = "SELECT n.id, n.user_id, n.title, n.content, n.is_public, n.created_at, n.updated_at, " +
                     "u.username FROM notes n JOIN users u ON n.user_id = u.id WHERE n.id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<Note> findByUserId(int userId) throws SQLException {
        String sql = "SELECT n.id, n.user_id, n.title, n.content, n.is_public, n.created_at, n.updated_at, " +
                     "u.username FROM notes n JOIN users u ON n.user_id = u.id " +
                     "WHERE n.user_id = ? ORDER BY n.updated_at DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, userId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapRow(rs));
                }
            }
        }
        return notes;
    }

    public List<Note> findPublicNotes() throws SQLException {
        String sql = "SELECT n.id, n.user_id, n.title, n.content, n.is_public, n.created_at, n.updated_at, " +
                     "u.username FROM notes n JOIN users u ON n.user_id = u.id " +
                     "WHERE n.is_public = 1 ORDER BY n.updated_at DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                notes.add(mapRow(rs));
            }
        }
        return notes;
    }

    public List<Note> search(String query, Integer userId) throws SQLException {
        List<Note> notes = new ArrayList<>();
        String sql;
        if (userId != null) {
            sql = "SELECT n.id, n.user_id, n.title, n.content, n.is_public, n.created_at, n.updated_at, " +
                  "u.username FROM notes n JOIN users u ON n.user_id = u.id " +
                  "WHERE (n.user_id = ? OR n.is_public = 1) AND (n.title LIKE ? OR n.content LIKE ?) " +
                  "ORDER BY n.updated_at DESC";
        } else {
            sql = "SELECT n.id, n.user_id, n.title, n.content, n.is_public, n.created_at, n.updated_at, " +
                  "u.username FROM notes n JOIN users u ON n.user_id = u.id " +
                  "WHERE n.is_public = 1 AND (n.title LIKE ? OR n.content LIKE ?) " +
                  "ORDER BY n.updated_at DESC";
        }
        String pattern = "%" + query + "%";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            if (userId != null) {
                pstmt.setInt(1, userId);
                pstmt.setString(2, pattern);
                pstmt.setString(3, pattern);
            } else {
                pstmt.setString(1, pattern);
                pstmt.setString(2, pattern);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapRow(rs));
                }
            }
        }
        return notes;
    }

    public List<Note> findTopRated(int limit) throws SQLException {
        String sql = "SELECT n.id, n.user_id, n.title, n.content, n.is_public, n.created_at, n.updated_at, " +
                     "u.username, AVG(r.rating) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.is_public = 1 " +
                     "GROUP BY n.id HAVING rating_count > 0 " +
                     "ORDER BY avg_rating DESC, rating_count DESC LIMIT ?";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Note note = mapRow(rs);
                    note.setAverageRating(rs.getDouble("avg_rating"));
                    note.setRatingCount(rs.getInt("rating_count"));
                    notes.add(note);
                }
            }
        }
        return notes;
    }

    public int create(int userId, String title, String content, boolean isPublic) throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, is_public) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, userId);
            pstmt.setString(2, title);
            pstmt.setString(3, content);
            pstmt.setInt(4, isPublic ? 1 : 0);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void update(int id, String title, String content, boolean isPublic) throws SQLException {
        String sql = "UPDATE notes SET title = ?, content = ?, is_public = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, title);
            pstmt.setString(2, content);
            pstmt.setInt(3, isPublic ? 1 : 0);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public void reassign(int noteId, int newUserId) throws SQLException {
        String sql = "UPDATE notes SET user_id = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, newUserId);
            pstmt.setInt(2, noteId);
            pstmt.executeUpdate();
        }
    }

    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM notes";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        return 0;
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getInt("id"));
        note.setUserId(rs.getInt("user_id"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        note.setPublic(rs.getInt("is_public") == 1);
        note.setAuthorUsername(rs.getString("username"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            try {
                note.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
            } catch (Exception e) {
                note.setCreatedAt(LocalDateTime.now());
            }
        }
        String updatedAt = rs.getString("updated_at");
        if (updatedAt != null) {
            try {
                note.setUpdatedAt(LocalDateTime.parse(updatedAt.replace(" ", "T")));
            } catch (Exception e) {
                note.setUpdatedAt(LocalDateTime.now());
            }
        }
        return note;
    }
}
