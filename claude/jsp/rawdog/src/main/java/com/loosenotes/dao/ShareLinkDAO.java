package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;

import java.sql.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ShareLinkDAO {
    private static final Logger LOGGER = Logger.getLogger(ShareLinkDAO.class.getName());

    public ShareLink findByToken(String token) {
        String sql = "SELECT * FROM share_links WHERE token = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding share link by token", e);
        }
        return null;
    }

    public ShareLink findByNoteId(int noteId) {
        String sql = "SELECT * FROM share_links WHERE note_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error finding share link by note", e);
        }
        return null;
    }

    public boolean create(ShareLink link) {
        String sql = "INSERT INTO share_links (note_id, token) VALUES (?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, link.getNoteId());
            ps.setString(2, link.getToken());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) link.setId(keys.getInt(1));
                }
                return true;
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating share link", e);
        }
        return false;
    }

    public boolean deleteByNoteId(int noteId) {
        String sql = "DELETE FROM share_links WHERE note_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting share link", e);
        }
        return false;
    }

    private ShareLink mapRow(ResultSet rs) throws SQLException {
        ShareLink link = new ShareLink();
        link.setId(rs.getInt("id"));
        link.setNoteId(rs.getInt("note_id"));
        link.setToken(rs.getString("token"));
        link.setCreatedAt(rs.getTimestamp("created_at"));
        return link;
    }
}
