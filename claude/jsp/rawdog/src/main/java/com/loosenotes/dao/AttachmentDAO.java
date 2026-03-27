package com.loosenotes.dao;

import com.loosenotes.model.Attachment;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class AttachmentDAO {
    private static final Logger LOGGER = Logger.getLogger(AttachmentDAO.class.getName());

    public Attachment findById(int id) {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding attachment by id", e);
        }
        return null;
    }

    public List<Attachment> findByNoteId(int noteId) {
        List<Attachment> list = new ArrayList<>();
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY created_at ASC";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding attachments by note", e);
        }
        return list;
    }

    public boolean create(Attachment attachment) {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, file_size) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, attachment.getNoteId());
            ps.setString(2, attachment.getOriginalFilename());
            ps.setString(3, attachment.getStoredFilename());
            ps.setLong(4, attachment.getFileSize());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) attachment.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating attachment", e);
        }
        return false;
    }

    public boolean delete(int id) {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting attachment", e);
        }
        return false;
    }

    private Attachment mapRow(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getInt("id"));
        a.setNoteId(rs.getInt("note_id"));
        a.setOriginalFilename(rs.getString("original_filename"));
        a.setStoredFilename(rs.getString("stored_filename"));
        a.setFileSize(rs.getLong("file_size"));
        a.setCreatedAt(rs.getTimestamp("created_at"));
        return a;
    }
}
