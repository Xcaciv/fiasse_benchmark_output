package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;
import com.loosenotes.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ShareLinkDAO {
    private static final Logger logger = LoggerFactory.getLogger(ShareLinkDAO.class);
    
    public Long create(ShareLink shareLink) throws SQLException {
        String sql = "INSERT INTO share_links (note_id, token, active, created_at, expires_at) VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, shareLink.getNoteId());
            stmt.setString(2, shareLink.getToken());
            stmt.setBoolean(3, shareLink.isActive());
            stmt.setTimestamp(4, Timestamp.valueOf(shareLink.getCreatedAt()));
            
            if (shareLink.getExpiresAt() != null) {
                stmt.setTimestamp(5, Timestamp.valueOf(shareLink.getExpiresAt()));
            } else {
                stmt.setNull(5, Types.TIMESTAMP);
            }
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        
        return null;
    }
    
    public Optional<ShareLink> findById(Long id) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToShareLink(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    public Optional<ShareLink> findByToken(String token) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE token = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, token);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToShareLink(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    public List<ShareLink> findByNoteId(Long noteId) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE note_id = ? ORDER BY created_at DESC";
        List<ShareLink> shareLinks = new ArrayList<>();
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, noteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    shareLinks.add(mapResultSetToShareLink(rs));
                }
            }
        }
        
        return shareLinks;
    }
    
    public Optional<ShareLink> findActiveByNoteId(Long noteId) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE note_id = ? AND active = TRUE AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP) LIMIT 1";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, noteId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToShareLink(rs));
                }
            }
        }
        
        return Optional.empty();
    }
    
    public void deactivate(Long id) throws SQLException {
        String sql = "UPDATE share_links SET active = FALSE WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }
    
    public void deactivateByNoteId(Long noteId) throws SQLException {
        String sql = "UPDATE share_links SET active = FALSE WHERE note_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, noteId);
            stmt.executeUpdate();
        }
    }
    
    public void delete(Long id) throws SQLException {
        String sql = "DELETE FROM share_links WHERE id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }
    
    private ShareLink mapResultSetToShareLink(ResultSet rs) throws SQLException {
        ShareLink shareLink = new ShareLink();
        shareLink.setId(rs.getLong("id"));
        shareLink.setNoteId(rs.getLong("note_id"));
        shareLink.setToken(rs.getString("token"));
        shareLink.setActive(rs.getBoolean("active"));
        shareLink.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            shareLink.setExpiresAt(expiresAt.toLocalDateTime());
        }
        
        return shareLink;
    }
}
