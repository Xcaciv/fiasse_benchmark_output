package com.loosenotes.dao;

import com.loosenotes.model.Note;
import com.loosenotes.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class NoteDAO {
    private static final Logger logger = LoggerFactory.getLogger(NoteDAO.class);
    
    public Long create(Note note) throws SQLException {
        String sql = "INSERT INTO notes (user_id, title, content, is_public, created_at, updated_at) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, note.getUserId());
            stmt.setString(2, note.getTitle());
            stmt.setString(3, note.getContent());
            stmt.setBoolean(4, note.isPublic());
            stmt.setTimestamp(5, Timestamp.valueOf(note.getCreatedAt()));
            stmt.setTimestamp(6, Timestamp.valueOf(note.getUpdatedAt()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return null;
    }
    
    public Optional<Note> findById(Long id) throws SQLException {
        String sql = "SELECT n.*, u.username as owner_username, u.email as owner_email " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id WHERE n.id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToNote(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    public List<Note> findByUserId(Long userId) throws SQLException {
        String sql = "SELECT n.*, u.username as owner_username, u.email as owner_email " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id " +
                     "WHERE n.user_id = ? ORDER BY n.updated_at DESC";
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        }
        
        return notes;
    }
    
    public List<Note> findPublicNotes() throws SQLException {
        String sql = "SELECT n.*, u.username as owner_username, u.email as owner_email " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id " +
                     "WHERE n.is_public = TRUE ORDER BY n.created_at DESC";
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                notes.add(mapResultSetToNote(rs));
            }
        }
        
        return notes;
    }
    
    public List<Note> search(String keyword, Long currentUserId) throws SQLException {
        String sql = "SELECT DISTINCT n.*, u.username as owner_username, u.email as owner_email " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE (n.title LIKE ? OR n.content LIKE ?) " +
                     "AND (n.is_public = TRUE OR n.user_id = ?) " +
                     "ORDER BY n.created_at DESC";
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setLong(3, currentUserId != null ? currentUserId : -1L);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    notes.add(mapResultSetToNote(rs));
                }
            }
        }
        
        return notes;
    }
    
    public List<Note> findTopRated(int limit) throws SQLException {
        String sql = "SELECT n.*, u.username as owner_username, u.email as owner_email, " +
                     "COALESCE(AVG(r.value), 0) as avg_rating, COUNT(r.id) as rating_count " +
                     "FROM notes n LEFT JOIN users u ON n.user_id = u.id " +
                     "LEFT JOIN ratings r ON n.id = r.note_id " +
                     "WHERE n.is_public = TRUE " +
                     "GROUP BY n.id " +
                     "HAVING COUNT(r.id) >= 3 " +
                     "ORDER BY avg_rating DESC, rating_count DESC " +
                     "LIMIT ?";
        List<Note> notes = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Note note = mapResultSetToNote(rs);
                    note.setAverageRating(rs.getDouble("avg_rating"));
                    notes.add(note);
                }
            }
        }
        
        return notes;
    }
    
    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM notes";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        
        return 0;
    }
    
    public int countByUserId(Long userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notes WHERE user_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
    
    public void update(Note note) throws SQLException {
        String sql = "UPDATE notes SET title = ?, content = ?, is_public = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, note.getTitle());
            stmt.setString(2, note.getContent());
            stmt.setBoolean(3, note.isPublic());
            stmt.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(5, note.getId());
            
            stmt.executeUpdate();
        }
    }
    
    public void updateOwner(Long noteId, Long newOwnerId) throws SQLException {
        String sql = "UPDATE notes SET user_id = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, newOwnerId);
            stmt.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(3, noteId);
            
            stmt.executeUpdate();
        }
    }
    
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM notes WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }
    
    private Note mapResultSetToNote(ResultSet rs) throws SQLException {
        Note note = new Note();
        note.setId(rs.getLong("id"));
        note.setUserId(rs.getLong("user_id"));
        note.setTitle(rs.getString("title"));
        note.setContent(rs.getString("content"));
        note.setPublic(rs.getBoolean("is_public"));
        note.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        note.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        String ownerUsername = rs.getString("owner_username");
        if (ownerUsername != null) {
            com.loosenotes.model.User owner = new com.loosenotes.model.User();
            owner.setId(note.getUserId());
            owner.setUsername(ownerUsername);
            note.setOwner(owner);
        }
        
        return note;
    }
}
