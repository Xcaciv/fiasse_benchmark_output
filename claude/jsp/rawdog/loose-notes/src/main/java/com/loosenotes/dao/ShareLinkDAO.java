package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;
import com.loosenotes.util.DBUtil;

import java.sql.*;
import java.time.LocalDateTime;

public class ShareLinkDAO {

    public ShareLink findByNoteId(int noteId) throws SQLException {
        String sql = "SELECT id, note_id, token, created_at FROM share_links WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public ShareLink findByToken(String token) throws SQLException {
        String sql = "SELECT id, note_id, token, created_at FROM share_links WHERE token = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, token);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public int create(int noteId, String token) throws SQLException {
        String sql = "INSERT INTO share_links (note_id, token) VALUES (?, ?)";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setInt(1, noteId);
            pstmt.setString(2, token);
            pstmt.executeUpdate();
            try (ResultSet keys = pstmt.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
        }
        return -1;
    }

    public void deleteByNoteId(int noteId) throws SQLException {
        String sql = "DELETE FROM share_links WHERE note_id = ?";
        try (Connection conn = DBUtil.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, noteId);
            pstmt.executeUpdate();
        }
    }

    private ShareLink mapRow(ResultSet rs) throws SQLException {
        ShareLink shareLink = new ShareLink();
        shareLink.setId(rs.getInt("id"));
        shareLink.setNoteId(rs.getInt("note_id"));
        shareLink.setToken(rs.getString("token"));
        String createdAt = rs.getString("created_at");
        if (createdAt != null) {
            try {
                shareLink.setCreatedAt(LocalDateTime.parse(createdAt.replace(" ", "T")));
            } catch (Exception e) {
                shareLink.setCreatedAt(LocalDateTime.now());
            }
        }
        return shareLink;
    }
}
