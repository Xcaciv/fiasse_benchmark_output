package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;

import java.sql.*;
import java.util.Optional;

public class ShareLinkDao {

    public long insert(Connection conn, ShareLink sl) throws SQLException {
        String sql = "INSERT INTO share_links (note_id, token) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, sl.getNoteId());
            ps.setString(2, sl.getToken());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public Optional<ShareLink> findByToken(Connection conn, String token) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE token = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<ShareLink> findByNoteId(Connection conn, long noteId) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE note_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public void deleteByNoteId(Connection conn, long noteId) throws SQLException {
        String sql = "DELETE FROM share_links WHERE note_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        }
    }

    private ShareLink mapRow(ResultSet rs) throws SQLException {
        ShareLink sl = new ShareLink();
        sl.setId(rs.getLong("id"));
        sl.setNoteId(rs.getLong("note_id"));
        sl.setToken(rs.getString("token"));
        sl.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return sl;
    }
}
