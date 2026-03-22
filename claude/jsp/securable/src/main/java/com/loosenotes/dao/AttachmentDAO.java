package com.loosenotes.dao;

import com.loosenotes.model.Attachment;
import com.loosenotes.util.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttachmentDAO {

    public long create(long noteId, String originalFilename, String storedFilename, long fileSize)
            throws SQLException {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, file_size) VALUES (?, ?, ?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, noteId);
            ps.setString(2, originalFilename);
            ps.setString(3, storedFilename);
            ps.setLong(4, fileSize);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        }
    }

    public Attachment findById(long id) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public List<Attachment> findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY uploaded_at DESC";
        List<Attachment> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Attachment map(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getLong("id"));
        a.setNoteId(rs.getLong("note_id"));
        a.setOriginalFilename(rs.getString("original_filename"));
        a.setStoredFilename(rs.getString("stored_filename"));
        a.setFileSize(rs.getLong("file_size"));
        Timestamp ts = rs.getTimestamp("uploaded_at");
        if (ts != null) a.setUploadedAt(ts.toLocalDateTime());
        return a;
    }
}
