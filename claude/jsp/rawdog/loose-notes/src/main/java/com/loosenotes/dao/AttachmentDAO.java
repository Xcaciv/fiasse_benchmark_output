package com.loosenotes.dao;

import com.loosenotes.model.Attachment;
import com.loosenotes.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AttachmentDAO {

    public Attachment findById(int id) throws SQLException {
        String sql = "SELECT id, note_id, original_filename, stored_filename, file_size, content_type, uploaded_at " +
                     "FROM attachments WHERE id = ?";
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

    public List<Attachment> findByNoteId(int noteId) throws SQLException {
        String sql = "SELECT id, note_id, original_filename, stored_filename, file_size, content_type, uploaded_at " +
                     "FROM attachments WHERE note_id = ? ORDER BY uploaded_at DESC";
        List<Attachment> attachments = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    attachments.add(mapRow(rs));
                }
            }
        }
        return attachments;
    }

    public int create(int noteId, String originalFilename, String storedFilename,
                      long fileSize, String contentType) throws SQLException {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, file_size, content_type) " +
                     "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, noteId);
            pstmt.setString(2, originalFilename);
            pstmt.setString(3, storedFilename);
            pstmt.setLong(4, fileSize);
            pstmt.setString(5, contentType);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            pstmt.executeUpdate();
        }
    }

    public void deleteByNoteId(int noteId) throws SQLException {
        String sql = "DELETE FROM attachments WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.executeUpdate();
        }
    }

    private Attachment mapRow(ResultSet rs) throws SQLException {
        Attachment attachment = new Attachment();
        attachment.setId(rs.getInt("id"));
        attachment.setNoteId(rs.getInt("note_id"));
        attachment.setOriginalFilename(rs.getString("original_filename"));
        attachment.setStoredFilename(rs.getString("stored_filename"));
        attachment.setFileSize(rs.getLong("file_size"));
        attachment.setContentType(rs.getString("content_type"));
        String uploadedAt = rs.getString("uploaded_at");
        if (uploadedAt != null) {
            try {
                attachment.setUploadedAt(LocalDateTime.parse(uploadedAt.replace(" ", "T")));
            } catch (Exception e) {
                attachment.setUploadedAt(LocalDateTime.now());
            }
        }
        return attachment;
    }
}
