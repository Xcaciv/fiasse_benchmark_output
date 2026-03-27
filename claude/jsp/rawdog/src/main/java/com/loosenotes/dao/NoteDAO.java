package com.loosenotes.dao;

import com.loosenotes.model.Note;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NoteDAO {
    private static final Logger LOGGER = Logger.getLogger(NoteDAO.class.getName());

    public Note findById(int id) {
        String sql = "SELECT n.*, u.username as owner_username, " +
                     "COALESCE(AVG(r.rating), 0.0) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n " +
                     "LEFT JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.id = ? GROUP BY n.id";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding note by id", e);
        }
        return null;
    }

    public List<Note> findByUserId(int userId) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT n.*, u.username as owner_username, " +
                     "COALESCE(AVG(r.rating), 0.0) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n " +
                     "LEFT JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.user_id = ? GROUP BY n.id ORDER BY n.updated_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding notes by user", e);
        }
        return notes;
    }

    public boolean create(Note note) {
        String sql = "INSERT INTO notes (title, content, is_public, user_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setInt(3, note.isPublic() ? 1 : 0);
            ps.setInt(4, note.getUserId());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) note.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating note", e);
        }
        return false;
    }

    public boolean update(Note note) {
        String sql = "UPDATE notes SET title = ?, content = ?, is_public = ?, " +
                     "updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, note.getTitle());
            ps.setString(2, note.getContent());
            ps.setInt(3, note.isPublic() ? 1 : 0);
            ps.setInt(4, note.getId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating note", e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting note", e);
        }
        return false;
    }

    public boolean reassignOwner(int noteId, int newUserId) {
        String sql = "UPDATE notes SET user_id = ? WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, newUserId);
            ps.setInt(2, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error reassigning note owner", e);
        }
        return false;
    }

    public List<Note> search(String keyword, int currentUserId) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT n.*, u.username as owner_username, " +
                     "COALESCE(AVG(r.rating), 0.0) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n " +
                     "LEFT JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE (n.title LIKE ? OR n.content LIKE ?) " +
                     "AND (n.user_id = ? OR n.is_public = 1) " +
                     "GROUP BY n.id ORDER BY n.updated_at DESC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            String like = "%" + keyword + "%";
            ps.setString(1, like);
            ps.setString(2, like);
            ps.setInt(3, currentUserId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error searching notes", e);
        }
        return notes;
    }

    public List<Note> findTopRated(int minRatings) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT n.*, u.username as owner_username, " +
                     "COALESCE(AVG(r.rating), 0.0) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n " +
                     "LEFT JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.is_public = 1 " +
                     "GROUP BY n.id " +
                     "HAVING COUNT(r.id) >= ? " +
                     "ORDER BY avg_rating DESC, rating_count DESC " +
                     "LIMIT 20";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, minRatings);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) notes.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding top rated notes", e);
        }
        return notes;
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM notes";
        try (Connection conn = DatabaseUtil.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error counting notes", e);
        }
        return 0;
    }

    private Note mapRow(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getInt("id"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        note.setPublic(rs.getInt("is_public") == 1);
        note.setUserId(rs.getInt("user_id"));
        note.setCreatedAt(rs.getTimestamp("created_at"));
        note.setUpdatedAt(rs.getTimestamp("updated_at"));
        note.setOwnerUsername(rs.getString("owner_username"));
        note.setAverageRating(rs.getDouble("avg_rating"));
        note.setRatingCount(rs.getInt("rating_count"));
        return note;
    }
}
