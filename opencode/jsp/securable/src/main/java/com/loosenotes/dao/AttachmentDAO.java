package com.loosenotes.dao;

import com.loosenotes.model.Attachment;
import com.loosenotes.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttachmentDAO {
    private static final Logger logger = LoggerFactory.getLogger(AttachmentDAO.class);
    
    public Long create(Attachment attachment) throws SQLException {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, content_type, file_size, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, attachment.getNoteId());
            stmt.setString(2, attachment.getOriginalFilename());
            stmt.setString(3, attachment.getStoredFilename());
            stmt.setString(4, attachment.getContentType());
            stmt.setLong(5, attachment.getFileSize());
            stmt.setTimestamp(6, Timestamp.valueOf(attachment.getCreatedAt()));
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return null;
    }
    
    public Optional<Attachment> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttachment(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    public Optional<Attachment> findByStoredFilename(String storedFilename) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE stored_filename = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, storedFilename);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToAttachment(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    public List<Attachment> findByNoteId(Long noteId) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at DESC";
        List<Attachment> attachments = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, noteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    attachments.add(mapResultSetToAttachment(rs));
                }
            }
        }
        
        return attachments;
    }
    
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM attachments WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }
    
    public void deleteByNoteId(Long noteId) throws SQLException {
        String sql = "DELETE FROM attachments WHERE note_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, noteId);
            stmt.executeUpdate();
        }
    }
    
    private Attachment mapResultSetToAttachment(ResultSet rs) throws SQLException {
        Attachment attachment = new Attachment();
        attachment.setId(rs.getLong("id"));
        attachment.setNoteId(rs.getLong("note_id"));
        attachment.setOriginalFilename(rs.getString("original_filename"));
        attachment.setStoredFilename(rs.getString("stored_filename"));
        attachment.setContentType(rs.getString("content_type"));
        attachment.setFileSize(rs.getLong("file_size"));
        attachment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return attachment;
    }
}
