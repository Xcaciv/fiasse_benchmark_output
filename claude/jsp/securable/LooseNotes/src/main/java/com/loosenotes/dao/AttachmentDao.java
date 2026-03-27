package com.loosenotes.dao;

import com.loosenotes.model.Attachment;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for Attachment entities.
 * SSEM: Integrity - parameterized queries, cascade delete via FK.
 * SSEM: Resilience - try-with-resources throughout.
 */
public class AttachmentDao {

    private final DataSource dataSource;

    public AttachmentDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Inserts a new attachment and returns the generated ID. */
    public long create(Attachment attachment) throws SQLException {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, "
            + "file_size, content_type) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, attachment.getNoteId());
            ps.setString(2, attachment.getOriginalFilename());
            ps.setString(3, attachment.getStoredFilename());
            ps.setLong(4, attachment.getFileSize());
            ps.setString(5, attachment.getContentType());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("Attachment creation failed: no generated key");
    }

    /** Finds an attachment by ID. */
    public Optional<Attachment> findById(long id) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Returns all attachments for a given note. */
    public List<Attachment> findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at ASC";
        List<Attachment> list = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    /** Deletes an attachment by ID. Caller is responsible for removing the file. */
    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private Attachment mapRow(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getLong("id"));
        a.setNoteId(rs.getLong("note_id"));
        a.setOriginalFilename(rs.getString("original_filename"));
        a.setStoredFilename(rs.getString("stored_filename"));
        a.setFileSize(rs.getLong("file_size"));
        a.setContentType(rs.getString("content_type"));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return a;
    }
}
