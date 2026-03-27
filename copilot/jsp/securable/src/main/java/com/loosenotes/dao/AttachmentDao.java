package com.loosenotes.dao;

import com.loosenotes.model.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the attachments table.
 */
public class AttachmentDao {

    private static final Logger log = LoggerFactory.getLogger(AttachmentDao.class);
    private final DatabaseManager dbManager;

    public AttachmentDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(Attachment attachment) throws SQLException {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, content_type, file_size, uploaded_at) "
                   + "VALUES (?,?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, attachment.getNoteId());
            ps.setString(2, attachment.getOriginalFilename());
            ps.setString(3, attachment.getStoredFilename());
            ps.setString(4, attachment.getContentType());
            ps.setLong(5, attachment.getFileSize());
            ps.setLong(6, attachment.getUploadedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    attachment.setId(keys.getLong(1));
                }
            }
        }
    }

    public Optional<Attachment> findById(long id) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public List<Attachment> findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY uploaded_at ASC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    public void deleteByNoteId(long noteId) throws SQLException {
        String sql = "DELETE FROM attachments WHERE note_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
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
        a.setUploadedAt(rs.getLong("uploaded_at"));
        return a;
    }

    private List<Attachment> mapRows(ResultSet rs) throws SQLException {
        List<Attachment> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
