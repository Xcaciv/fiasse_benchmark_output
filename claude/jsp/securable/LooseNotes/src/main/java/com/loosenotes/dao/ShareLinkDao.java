package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;

import java.sql.*;
import java.util.Optional;

/**
 * Data access for the share_links table.
 *
 * SSEM notes:
 * - Authenticity: token lookup uses parameterized query only.
 * - Integrity: deleting by noteId revokes all share links for a note.
 */
public class ShareLinkDao {

    private final DatabaseManager db;

    public ShareLinkDao(DatabaseManager db) {
        this.db = db;
    }

    /** Finds a share link by its token. */
    public Optional<ShareLink> findByToken(String token) throws SQLException {
        String sql = "SELECT id, note_id, token, created_at FROM share_links WHERE token = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Finds the share link for a specific note (if any). */
    public Optional<ShareLink> findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT id, note_id, token, created_at FROM share_links WHERE note_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Inserts a new share link. Returns the generated ID. */
    public long insert(ShareLink link) throws SQLException {
        String sql = "INSERT INTO share_links (note_id, token) VALUES (?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, link.getNoteId());
            ps.setString(2, link.getToken());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No generated key for share_link insert");
            }
        }
    }

    /** Deletes all share links for a note (revoke). */
    public void deleteByNoteId(long noteId) throws SQLException {
        String sql = "DELETE FROM share_links WHERE note_id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        }
    }

    private ShareLink mapRow(ResultSet rs) throws SQLException {
        ShareLink sl = new ShareLink();
        sl.setId(rs.getLong("id"));
        sl.setNoteId(rs.getLong("note_id"));
        sl.setToken(rs.getString("token"));
        sl.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        return sl;
    }
}
