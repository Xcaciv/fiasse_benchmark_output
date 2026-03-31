package com.loosenotes.dao;

import com.loosenotes.model.Attachment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the attachments table.
 *
 * SSEM notes:
 * - Integrity: storedName (UUID) is the only file system reference; originalName is display-only.
 * - Resilience: try-with-resources on all connections.
 */
public class AttachmentDao {

    private final DatabaseManager db;

    public AttachmentDao(DatabaseManager db) {
        this.db = db;
    }

    public Optional<Attachment> findById(long id) throws SQLException {
        String sql = "SELECT id, note_id, original_name, stored_name, mime_type, "
                + "file_size, uploaded_at FROM attachments WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Attachment> findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT id, note_id, original_name, stored_name, mime_type, "
                + "file_size, uploaded_at FROM attachments WHERE note_id = ? ORDER BY uploaded_at";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Attachment> list = new ArrayList<>();
                while (rs.next()) list.add(mapRow(rs));
                return list;
            }
        }
    }

    public long insert(Attachment a) throws SQLException {
        String sql = "INSERT INTO attachments (note_id, original_name, stored_name, "
                + "mime_type, file_size) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.getNoteId());
            ps.setString(2, a.getOriginalName());
            ps.setString(3, a.getStoredName());
            ps.setString(4, a.getMimeType());
            ps.setLong(5, a.getFileSizeBytes());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No generated key for attachment insert");
            }
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Attachment mapRow(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getLong("id"));
        a.setNoteId(rs.getLong("note_id"));
        a.setOriginalName(rs.getString("original_name"));
        a.setStoredName(rs.getString("stored_name"));
        a.setMimeType(rs.getString("mime_type"));
        a.setFileSizeBytes(rs.getLong("file_size"));
        a.setUploadedAt(rs.getTimestamp("uploaded_at").toInstant());
        return a;
    }
}
