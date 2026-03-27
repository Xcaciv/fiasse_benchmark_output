package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;

/**
 * Data access layer for ShareLink entities.
 * SSEM: Authenticity - tokens stored as hashes (see SecureTokenUtil).
 * SSEM: Resilience - try-with-resources throughout.
 */
public class ShareLinkDao {

    private final DataSource dataSource;

    public ShareLinkDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Creates a new share link and returns the generated ID. */
    public long create(ShareLink link) throws SQLException {
        // Deactivate any existing active links for this note before creating new one
        deactivateByNoteId(link.getNoteId());
        String sql = "INSERT INTO share_links (note_id, token, is_active) VALUES (?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, link.getNoteId());
            ps.setString(2, link.getToken());
            ps.setBoolean(3, link.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("ShareLink creation failed: no generated key");
    }

    /**
     * Finds an active share link by token hash.
     * The token parameter must be the SHA-256 hash of the raw URL token.
     */
    public Optional<ShareLink> findActiveByToken(String tokenHash) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE token = ? AND is_active = TRUE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Returns the active share link for a note, if any. */
    public Optional<ShareLink> findActiveByNoteId(long noteId) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE note_id = ? AND is_active = TRUE";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Deactivates all share links for a note (revoke). */
    public void deactivateByNoteId(long noteId) throws SQLException {
        String sql = "UPDATE share_links SET is_active = FALSE WHERE note_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        }
    }

    private ShareLink mapRow(ResultSet rs) throws SQLException {
        ShareLink link = new ShareLink();
        link.setId(rs.getLong("id"));
        link.setNoteId(rs.getLong("note_id"));
        link.setToken(rs.getString("token"));
        link.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        link.setActive(rs.getBoolean("is_active"));
        return link;
    }
}
