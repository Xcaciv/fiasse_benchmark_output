package com.loosenotes.dao.impl;

import com.loosenotes.dao.AttachmentDao;
import com.loosenotes.model.Attachment;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AttachmentDaoImpl implements AttachmentDao {

    private static final Logger log = LoggerFactory.getLogger(AttachmentDaoImpl.class);
    private final DatabaseManager db;

    public AttachmentDaoImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public long insert(Attachment a) {
        final String sql =
            "INSERT INTO attachments (note_id, original_filename, stored_filename, " +
            "file_size, mime_type) VALUES (?,?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1,   a.getNoteId());
            ps.setString(2, a.getOriginalFilename());
            ps.setString(3, a.getStoredFilename());
            ps.setLong(4,   a.getFileSize());
            ps.setString(5, a.getMimeType());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            log.error("insert attachment failed", e);
            throw new RuntimeException("Database error inserting attachment", e);
        }
    }

    @Override
    public Optional<Attachment> findById(long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM attachments WHERE id=?")) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("findById attachment failed", e);
            return Optional.empty();
        }
    }

    @Override
    public List<Attachment> findByNoteId(long noteId) {
        List<Attachment> results = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM attachments WHERE note_id=? ORDER BY uploaded_at")) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByNoteId attachment failed", e);
        }
        return results;
    }

    @Override
    public boolean delete(long id) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM attachments WHERE id=?")) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("delete attachment failed", e);
            return false;
        }
    }

    private Attachment mapRow(ResultSet rs) throws SQLException {
        String uploadedStr = rs.getString("uploaded_at");
        LocalDateTime uploaded = uploadedStr != null
                ? LocalDateTime.parse(uploadedStr.replace(" ", "T"))
                : LocalDateTime.now();
        return new Attachment(
            rs.getLong("id"),
            rs.getLong("note_id"),
            rs.getString("original_filename"),
            rs.getString("stored_filename"),
            rs.getLong("file_size"),
            rs.getString("mime_type"),
            uploaded
        );
    }
}
