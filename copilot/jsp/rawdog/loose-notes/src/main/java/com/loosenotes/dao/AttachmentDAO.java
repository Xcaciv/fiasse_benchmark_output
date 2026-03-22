package com.loosenotes.dao;

import com.loosenotes.model.Attachment;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AttachmentDAO {

    public void addAttachment(int noteId, String originalFilename, String storedFilename) throws Exception {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, uploaded_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.setString(2, originalFilename);
            ps.setString(3, storedFilename);
            ps.setString(4, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    public List<Attachment> getAttachmentsByNote(int noteId) throws Exception {
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY uploaded_at DESC";
        List<Attachment> list = new ArrayList<>();
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapAttachment(rs));
            }
        }
        return list;
    }

    public Attachment getAttachmentById(int id) throws Exception {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapAttachment(rs);
            }
        }
        return null;
    }

    public void deleteAttachment(int id) throws Exception {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void deleteAttachmentsByNote(int noteId) throws Exception {
        String sql = "DELETE FROM attachments WHERE note_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
        }
    }

    private Attachment mapAttachment(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getInt("id"));
        a.setNoteId(rs.getInt("note_id"));
        a.setOriginalFilename(rs.getString("original_filename"));
        a.setStoredFilename(rs.getString("stored_filename"));
        a.setUploadedAt(rs.getString("uploaded_at"));
        return a;
    }
}
