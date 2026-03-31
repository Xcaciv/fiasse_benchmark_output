package com.loosenotes.dao;

import com.loosenotes.model.Rating;
import com.loosenotes.model.User;
import com.loosenotes.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class RatingDAO {
    private static final Logger logger = LoggerFactory.getLogger(RatingDAO.class);
    
    public Long create(Rating rating) throws SQLException {
        String sql = "INSERT INTO ratings (note_id, user_id, value, comment, created_at, updated_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, rating.getNoteId());
            stmt.setLong(2, rating.getUserId());
            stmt.setInt(3, rating.getValue());
            stmt.setString(4, rating.getComment());
            stmt.setTimestamp(5, Timestamp.valueOf(rating.getCreatedAt()));
            stmt.setTimestamp(6, Timestamp.valueOf(rating.getUpdatedAt()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return null;
    }
    
    public Optional<Rating> findById(Long id) throws SQLException {
        String sql = "SELECT r.*, u.username FROM ratings r LEFT JOIN users u ON r.user_id = u.id WHERE r.id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRating(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    public Optional<Rating> findByNoteIdAndUserId(Long noteId, Long userId) throws SQLException {
        String sql = "SELECT r.*, u.username FROM ratings r LEFT JOIN users u ON r.user_id = u.id WHERE r.note_id = ? AND r.user_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, noteId);
            stmt.setLong(2, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRating(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    public List<Rating> findByNoteId(Long noteId) throws SQLException {
        String sql = "SELECT r.*, u.username FROM ratings r LEFT JOIN users u ON r.user_id = u.id " +
                     "WHERE r.note_id = ? ORDER BY r.created_at DESC";
        List<Rating> ratings = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, noteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ratings.add(mapResultSetToRating(rs));
                }
            }
        }
        
        return ratings;
    }
    
    public Double getAverageRating(Long noteId) throws SQLException {
        String sql = "SELECT COALESCE(AVG(value), 0) as avg_rating FROM ratings WHERE note_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, noteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("avg_rating");
                }
            }
        }
        
        return 0.0;
    }
    
    public int getRatingCount(Long noteId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM ratings WHERE note_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, noteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        
        return 0;
    }
    
    public void update(Rating rating) throws SQLException {
        String sql = "UPDATE ratings SET value = ?, comment = ?, updated_at = ? WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, rating.getValue());
            stmt.setString(2, rating.getComment());
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setLong(4, rating.getId());
            
            stmt.executeUpdate();
        }
    }
    
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM ratings WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }
    
    private Rating mapResultSetToRating(ResultSet rs) throws SQLException {
        Rating rating = new Rating();
        rating.setId(rs.getLong("id"));
        rating.setNoteId(rs.getLong("note_id"));
        rating.setUserId(rs.getLong("user_id"));
        rating.setValue(rs.getInt("value"));
        rating.setComment(rs.getString("comment"));
        rating.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        rating.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        
        String username = rs.getString("username");
        if (username != null) {
            User user = new User();
            user.setId(rating.getUserId());
            user.setUsername(username);
            rating.setUser(user);
        }
        
        return rating;
    }
}
