package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;

import java.sql.*;
import java.time.LocalDateTime;

public class ShareLinkDAO {

    private ShareLink mapRow(ResultSet rs) throws SQLException {
        ShareLink sl = new ShareLink();
        sl.setId(rs.getInt("id"));
        sl.setNoteId(rs.getInt("note_id"));
        sl.setToken(rs.getString("token"));
        sl.setCreatedAt(rs.getString("created_at"));
        return sl;
    }

    public ShareLink findByToken(String token) {
        String sql = "SELECT * FROM share_links WHERE token = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public ShareLink findByNoteId(int noteId) {
        String sql = "SELECT * FROM share_links WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public void create(ShareLink link) {
        String sql = "INSERT INTO share_links (note_id, token, created_at) VALUES (?,?,?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, link.getNoteId());
            ps.setString(2, link.getToken());
            ps.setString(3, LocalDateTime.now().toString());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) link.setId(keys.getInt(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteByNoteId(int noteId) {
        String sql = "DELETE FROM share_links WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, noteId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
