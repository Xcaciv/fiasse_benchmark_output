package com.loosenotes.dao;

import com.loosenotes.model.Attachment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttachmentDao {

    public long insert(Connection conn, Attachment a) throws SQLException {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, content_type, file_size) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, a.getNoteId());
            ps.setString(2, a.getOriginalFilename());
            ps.setString(3, a.getStoredFilename());
            ps.setString(4, a.getContentType());
            ps.setLong(5, a.getFileSize());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public Optional<Attachment> findById(Connection conn, long id) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Attachment> findByNoteId(Connection conn, long noteId) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY uploaded_at";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            List<Attachment> list = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
            return list;
        }
    }

    public int countByNoteId(Connection conn, long noteId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM attachments WHERE note_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    public void deleteById(Connection conn, long id) throws SQLException {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void deleteByNoteId(Connection conn, long noteId) throws SQLException {
        String sql = "DELETE FROM attachments WHERE note_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        }
    }

    private Attachment mapRow(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getLong("id"));
        a.setNoteId(rs.getLong("note_id"));
        a.setOriginalFilename(rs.getString("original_filename"));
        a.setStoredFilename(rs.getString("stored_filename"));
        a.setContentType(rs.getString("content_type"));
        a.setFileSize(rs.getLong("file_size"));
        a.setUploadedAt(rs.getTimestamp("uploaded_at").toLocalDateTime());
        return a;
    }
}
