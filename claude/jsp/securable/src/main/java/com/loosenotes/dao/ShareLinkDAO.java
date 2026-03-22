package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;
import com.loosenotes.util.DatabaseManager;

import java.security.SecureRandom;
import java.sql.*;
import java.util.Base64;

public class ShareLinkDAO {

    private static final SecureRandom RANDOM = new SecureRandom();

    public ShareLink findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE note_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public ShareLink findByToken(String token) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE token = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Create or regenerate the share link for a note. Returns the new token. */
    public String createOrReplace(long noteId) throws SQLException {
        // Delete existing link first
        deleteByNoteId(noteId);

        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        String sql = "INSERT INTO share_links (note_id, token) VALUES (?, ?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.setString(2, token);
            ps.executeUpdate();
        }
        return token;
    }

    public void deleteByNoteId(long noteId) throws SQLException {
        String sql = "DELETE FROM share_links WHERE note_id = ?";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        }
    }

    private ShareLink map(ResultSet rs) throws SQLException {
        ShareLink sl = new ShareLink();
        sl.setId(rs.getLong("id"));
        sl.setNoteId(rs.getLong("note_id"));
        sl.setToken(rs.getString("token"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) sl.setCreatedAt(ts.toLocalDateTime());
        return sl;
    }
}
