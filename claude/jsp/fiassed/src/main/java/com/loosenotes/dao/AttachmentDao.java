package com.loosenotes.dao;

import com.loosenotes.model.Attachment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code attachments} table.
 * storedFilename is a UUID-based server-generated value and is never
 * derived from user input (F-05, Integrity).
 */
public class AttachmentDao {

    private static final Logger log = LoggerFactory.getLogger(AttachmentDao.class);

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ------------------------------------------------------------------ reads

    public Attachment findById(Long id) {
        final String sql = "SELECT * FROM attachments WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findById failed for id={}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    public List<Attachment> findByNoteId(Long noteId) {
        final String sql =
            "SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at ASC";
        List<Attachment> list = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findByNoteId failed noteId={}: {}", noteId, e.getMessage(), e);
        }
        return list;
    }

    /**
     * Returns the total bytes stored across all attachments belonging to notes
     * owned by the given user (join query — no in-memory aggregation).
     */
    public long getTotalSizeByUserId(Long userId) {
        final String sql =
            "SELECT COALESCE(SUM(a.file_size), 0) " +
            "FROM attachments a " +
            "JOIN notes n ON n.id = a.note_id " +
            "WHERE n.user_id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        } catch (SQLException e) {
            log.error("getTotalSizeByUserId failed userId={}: {}", userId, e.getMessage(), e);
            return 0L;
        }
    }

    // ----------------------------------------------------------------- writes

    public boolean create(Attachment attachment) {
        final String sql =
            "INSERT INTO attachments " +
            "(note_id, stored_filename, original_filename, content_type, file_size) " +
            "VALUES (?, ?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, attachment.getNoteId());
            ps.setString(2, attachment.getStoredFilename());
            ps.setString(3, attachment.getOriginalFilename());
            ps.setString(4, attachment.getContentType());
            ps.setLong(5, attachment.getFileSize());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) attachment.setId(keys.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("create attachment failed noteId={}: {}", attachment.getNoteId(), e.getMessage(), e);
        }
        return false;
    }

    public boolean delete(Long id) {
        final String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("delete attachment failed id={}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    // --------------------------------------------------------------- helpers

    private Attachment mapRow(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getLong("id"));
        a.setNoteId(rs.getLong("note_id"));
        a.setStoredFilename(rs.getString("stored_filename"));
        a.setOriginalFilename(rs.getString("original_filename"));
        a.setContentType(rs.getString("content_type"));
        a.setFileSize(rs.getLong("file_size"));
        a.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return a;
    }
}
