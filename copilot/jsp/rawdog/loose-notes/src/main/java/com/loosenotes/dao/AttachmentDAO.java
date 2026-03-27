package com.loosenotes.dao;

import com.loosenotes.model.Attachment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AttachmentDAO {

    private Attachment mapRow(ResultSet rs) throws SQLException {
        Attachment a = new Attachment();
        a.setId(rs.getInt("id"));
        a.setNoteId(rs.getInt("note_id"));
        a.setOriginalFilename(rs.getString("original_filename"));
        a.setStoredFilename(rs.getString("stored_filename"));
        a.setFileSize(rs.getLong("file_size"));
        a.setUploadedAt(rs.getString("uploaded_at"));
        return a;
    }

    public Attachment findById(int id) {
        String sql = "SELECT * FROM attachments WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public List<Attachment> findByNoteId(int noteId) {
        String sql = "SELECT * FROM attachments WHERE note_id = ? ORDER BY uploaded_at ASC";
        List<Attachment> list = new ArrayList<>();
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public void create(Attachment attachment) {
        String sql = "INSERT INTO attachments (note_id, original_filename, stored_filename, file_size, uploaded_at) VALUES (?,?,?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, attachment.getNoteId());
            ps.setString(2, attachment.getOriginalFilename());
            ps.setString(3, attachment.getStoredFilename());
            ps.setLong(4, attachment.getFileSize());
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) attachment.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete(int id) {
        String sql = "DELETE FROM attachments WHERE id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteByNoteId(int noteId) {
        String sql = "DELETE FROM attachments WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
