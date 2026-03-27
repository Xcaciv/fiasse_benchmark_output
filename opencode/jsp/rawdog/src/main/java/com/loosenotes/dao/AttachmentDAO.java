package com.loosenotes.dao;

import com.loosenotes.model.Attachment;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttachmentDAO {

    public boolean createAttachment(Attachment attachment) {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, file_path, file_size, content_type) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, attachment.getNoteId());
            stmt.setString(2, attachment.getOriginalFilename());
            stmt.setString(3, attachment.getStoredFilename());
            stmt.setString(4, attachment.getFilePath());
            stmt.setLong(5, attachment.getFileSize());
            stmt.setString(6, attachment.getContentType());
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    attachment.setId(rs.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Optional<Attachment> findById(Long id) {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToAttachment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Attachment> findByNoteId(Long noteId) {
        List<Attachment> attachments = new ArrayList<>();
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at DESC";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                attachments.add(mapResultSetToAttachment(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return attachments;
    }

    public boolean deleteAttachment(Long id) {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteByNoteId(Long noteId) {
        String sql = "DELETE FROM attachments WHERE note_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private Attachment mapResultSetToAttachment(ResultSet rs) throws SQLException {
        Attachment attachment = new Attachment();
        attachment.setId(rs.getLong("id"));
        attachment.setNoteId(rs.getLong("note_id"));
        attachment.setOriginalFilename(rs.getString("original_filename"));
        attachment.setStoredFilename(rs.getString("stored_filename"));
        attachment.setFilePath(rs.getString("file_path"));
        attachment.setFileSize(rs.getLong("file_size"));
        attachment.setContentType(rs.getString("content_type"));
        attachment.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return attachment;
    }
}
