package com.loosenotes.dao;

import com.loosenotes.model.Note;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NoteDAO {

    private Note mapRow(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getInt("id"));
        note.setUserId(rs.getInt("user_id"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        note.setVisibility(rs.getString("visibility"));
        note.setCreatedAt(rs.getString("created_at"));
        note.setUpdatedAt(rs.getString("updated_at"));
        return note;
    }

    public Note findById(int id) {
        String sql = "SELECT * FROM notes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Note> findByUserId(int userId) {
        String sql = "SELECT * FROM notes WHERE user_id = ? ORDER BY created_at DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return notes;
    }

    public List<Note> findRecentByUserId(int userId, int limit) {
        String sql = "SELECT * FROM notes WHERE user_id = ? ORDER BY updated_at DESC LIMIT ?";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return notes;
    }

    public int countByUserId(int userId) {
        String sql = "SELECT COUNT(*) FROM notes WHERE user_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public void create(Note note) {
        String sql = "INSERT INTO notes (user_id, title, content, visibility, created_at, updated_at) VALUES (?,?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String now = LocalDateTime.now().toString();
            ps.setInt(1, note.getUserId());
            ps.setString(2, note.getTitle());
            ps.setString(3, note.getContent());
            ps.setString(4, note.getVisibility() != null ? note.getVisibility() : "PRIVATE");
            ps.setString(5, now);
            ps.setString(6, now);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) note.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update(Note note) {
        String sql = "UPDATE notes SET title = ?, content = ?, visibility = ?, updated_at = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setString(3, note.getVisibility());
            ps.setString(4, LocalDateTime.now().toString());
            ps.setInt(5, note.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateUserId(int noteId, int newUserId) {
        String sql = "UPDATE notes SET user_id = ? WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newUserId);
            ps.setInt(2, noteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public List<Note> searchNotes(String keyword, int userId) {
        String sql = "SELECT DISTINCT n.* FROM notes n " +
                     "WHERE (n.user_id = ? OR n.visibility = 'PUBLIC') " +
                     "AND (LOWER(n.title) LIKE LOWER(?) OR LOWER(n.content) LIKE LOWER(?)) " +
                     "ORDER BY n.created_at DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String kw = "%" + keyword + "%";
            ps.setInt(1, userId);
            ps.setString(2, kw);
            ps.setString(3, kw);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return notes;
    }

    public List<Note> findPublicTopRated(int minRatings) {
        String sql = "SELECT n.*, u.username, AVG(r.rating) AS avg_rating, COUNT(r.id) AS rating_count " +
                     "FROM notes n " +
                     "JOIN users u ON n.user_id = u.id " +
                     "JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.visibility = 'PUBLIC' " +
                     "GROUP BY n.id " +
                     "HAVING COUNT(r.id) >= ? " +
                     "ORDER BY avg_rating DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minRatings);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Note note = mapRow(rs);
                    note.setUsername(rs.getString("username"));
                    note.setAvgRating(rs.getDouble("avg_rating"));
                    note.setRatingCount(rs.getInt("rating_count"));
                    notes.add(note);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return notes;
    }

    public int countAll() {
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM notes")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return 0;
    }

    public List<Note> findAllForAdmin() {
        String sql = "SELECT n.*, u.username FROM notes n " +
                     "JOIN users u ON n.user_id = u.id ORDER BY n.created_at DESC";
        List<Note> notes = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Note note = mapRow(rs);
                note.setUsername(rs.getString("username"));
                notes.add(note);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return notes;
    }
}
