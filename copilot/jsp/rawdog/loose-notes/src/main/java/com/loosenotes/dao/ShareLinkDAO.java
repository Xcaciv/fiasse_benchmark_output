package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;
import java.sql.*;
import java.time.LocalDateTime;

public class ShareLinkDAO {

    public void createShareLink(int noteId, String token) throws Exception {
        String sql = "INSERT INTO share_links (note_id, token, created_at) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.setString(2, token);
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }

    public ShareLink getShareLinkByNote(int noteId) throws Exception {
        String sql = "SELECT * FROM share_links WHERE note_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapShareLink(rs);
            }
        }
        return null;
    }

    public ShareLink getShareLinkByToken(String token) throws Exception {
        String sql = "SELECT * FROM share_links WHERE token = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapShareLink(rs);
            }
        }
        return null;
    }

    public void deleteShareLinkByNote(int noteId) throws Exception {
        String sql = "DELETE FROM share_links WHERE note_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
        }
    }

    public void updateShareLink(int noteId, String newToken) throws Exception {
        String sql = "UPDATE share_links SET token = ?, created_at = ? WHERE note_id = ?";
        try (Connection conn = DatabaseUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newToken);
            ps.setString(2, LocalDateTime.now().toString());
            ps.setInt(3, noteId);
            ps.executeUpdate();
        }
    }

    private ShareLink mapShareLink(ResultSet rs) throws SQLException {
        ShareLink sl = new ShareLink();
        sl.setId(rs.getInt("id"));
        sl.setNoteId(rs.getInt("note_id"));
        sl.setToken(rs.getString("token"));
        sl.setCreatedAt(rs.getString("created_at"));
        return sl;
    }
}
