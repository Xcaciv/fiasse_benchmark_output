package com.loosenotes.dao;

import com.loosenotes.model.Note;
import com.loosenotes.model.User;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteDAO {
    
    private final UserDAO userDAO = new UserDAO();

    public boolean createNote(Note note) {
        String sql = "INSERT INTO notes (user_id, title, content, is_public, share_token, share_enabled) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, note.getUserId());
            stmt.setString(2, note.getTitle());
            stmt.setString(3, note.getContent());
            stmt.setBoolean(4, note.isPublic());
            stmt.setString(5, note.getShareToken());
            stmt.setBoolean(6, note.isShareEnabled());
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    note.setId(rs.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Optional<Note> findById(Long id) {
        String sql = "SELECT n.*, u.username as owner_username, u.email as owner_email " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id WHERE n.id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Note note = mapResultSetToNote(rs);
                populateNoteDetails(conn, note);
                return Optional.of(note);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Note> findByUserId(Long userId) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT n.*, u.username as owner_username " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id WHERE n.user_id = ? ORDER BY n.updated_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Note note = mapResultSetToNote(rs);
                populateNoteDetails(conn, note);
                notes.add(note);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    public List<Note> findPublicNotes() {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT n.*, u.username as owner_username " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id WHERE n.is_public = TRUE ORDER BY n.created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Note note = mapResultSetToNote(rs);
                populateNoteDetails(conn, note);
                notes.add(note);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    public List<Note> searchNotes(String query, Long currentUserId) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT n.*, u.username as owner_username, " +
                     "AVG(r.rating) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE (n.is_public = TRUE OR n.user_id = ?) " +
                     "AND (n.title LIKE ? OR n.content LIKE ?) " +
                     "GROUP BY n.id ORDER BY n.updated_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            String searchPattern = "%" + query + "%";
            stmt.setLong(1, currentUserId != null ? currentUserId : -1);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Note note = mapResultSetWithRating(rs);
                populateNoteDetails(conn, note);
                notes.add(note);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    public List<Note> findTopRated(int minRatings) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT n.*, u.username as owner_username, " +
                     "AVG(r.rating) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.is_public = TRUE " +
                     "GROUP BY n.id HAVING COUNT(r.id) >= ? " +
                     "ORDER BY avg_rating DESC, rating_count DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, minRatings);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Note note = mapResultSetWithRating(rs);
                populateNoteDetails(conn, note);
                notes.add(note);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    public List<Note> findRecent(int limit) {
        List<Note> notes = new ArrayList<>();
        String sql = "SELECT n.*, u.username as owner_username, " +
                     "AVG(r.rating) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.is_public = TRUE " +
                     "GROUP BY n.id ORDER BY n.created_at DESC LIMIT ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Note note = mapResultSetWithRating(rs);
                populateNoteDetails(conn, note);
                notes.add(note);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return notes;
    }

    public boolean updateNote(Note note) {
        String sql = "UPDATE notes SET title = ?, content = ?, is_public = ?, share_token = ?, share_enabled = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getContent());
            stmt.setBoolean(3, note.isPublic());
            stmt.setString(4, note.getShareToken());
            stmt.setBoolean(5, note.isShareEnabled());
            stmt.setLong(6, note.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteNote(Long id) {
        String sql = "DELETE FROM notes WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean reassignOwner(Long noteId, Long newOwnerId) {
        String sql = "UPDATE notes SET user_id = ?, updated_at = NOW() WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, newOwnerId);
            stmt.setLong(2, noteId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public int getTotalCount() {
        String sql = "SELECT COUNT(*) FROM notes";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public int getNoteCountByUser(Long userId) {
        String sql = "SELECT COUNT(*) FROM notes WHERE user_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    private void populateNoteDetails(Connection conn, Note note) throws SQLException {
        userDAO.findById(note.getUserId()).ifPresent(note::setOwner);
        
        String ratingSql = "SELECT AVG(rating), COUNT(*) FROM ratings WHERE note_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(ratingSql)) {
            stmt.setLong(1, note.getId());
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                note.setAverageRating(rs.getDouble(1));
                note.setRatingCount(rs.getInt(2));
            }
        }
    }

    private Note mapResultSetToNote(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getLong("id"));
        note.setUserId(rs.getLong("user_id"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        note.setPublic(rs.getBoolean("is_public"));
        note.setShareToken(rs.getString("share_token"));
        note.setShareEnabled(rs.getBoolean("share_enabled"));
        note.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp updatedAt = rs.getTimestamp("updated_at");
        if (updatedAt != null) {
            note.setUpdatedAt(updatedAt.toLocalDateTime());
        }
        
        User owner = new User();
        owner.setUsername(rs.getString("owner_username"));
        note.setOwner(owner);
        
        return note;
    }

    private Note mapResultSetWithRating(ResultSet rs) throws SQLException {
        Note note = mapResultSetToNote(rs);
        double avgRating = rs.getDouble("avg_rating");
        if (!rs.wasNull()) {
            note.setAverageRating(avgRating);
        }
        note.setRatingCount(rs.getInt("rating_count"));
        return note;
    }
}
