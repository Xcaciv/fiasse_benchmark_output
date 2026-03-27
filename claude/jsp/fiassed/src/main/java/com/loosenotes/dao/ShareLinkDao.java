package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code share_links} table.
 * The raw share token is never stored; only its SHA-256 hash is persisted (F-08).
 * Revocation is performed by setting revoked_at to the current timestamp.
 */
public class ShareLinkDao {

    private static final Logger log = LoggerFactory.getLogger(ShareLinkDao.class);

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ------------------------------------------------------------------ reads

    /**
     * Looks up a share link by the SHA-256 hash of the token.
     * The caller is responsible for verifying that revokedAt is null before granting access.
     */
    public ShareLink findByTokenHash(String tokenHash) {
        final String sql = "SELECT * FROM share_links WHERE token_hash = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findByTokenHash failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public ShareLink findById(Long id) {
        final String sql = "SELECT * FROM share_links WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findById failed id={}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    /** Returns all non-revoked links for the given note. */
    public List<ShareLink> findActiveByNoteId(Long noteId) {
        final String sql =
            "SELECT * FROM share_links WHERE note_id = ? AND revoked_at IS NULL " +
            "ORDER BY created_at DESC";
        List<ShareLink> list = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findActiveByNoteId failed noteId={}: {}", noteId, e.getMessage(), e);
        }
        return list;
    }

    /**
     * Counts active (non-revoked) share links across all notes owned by userId.
     * Used by rate-limiting logic to cap link creation per user.
     */
    public int countActiveByUserId(Long userId) {
        final String sql =
            "SELECT COUNT(*) FROM share_links sl " +
            "JOIN notes n ON n.id = sl.note_id " +
            "WHERE n.user_id = ? AND sl.revoked_at IS NULL";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            log.error("countActiveByUserId failed userId={}: {}", userId, e.getMessage(), e);
            return 0;
        }
    }

    // ----------------------------------------------------------------- writes

    public boolean create(ShareLink link) {
        final String sql =
            "INSERT INTO share_links (note_id, token_hash) VALUES (?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, link.getNoteId());
            ps.setString(2, link.getTokenHash());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) link.setId(keys.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("create share link failed noteId={}: {}", link.getNoteId(), e.getMessage(), e);
        }
        return false;
    }

    /** Revokes a single link by recording revoked_at = now(). */
    public boolean revoke(Long id) {
        final String sql =
            "UPDATE share_links SET revoked_at = CURRENT_TIMESTAMP " +
            "WHERE id = ? AND revoked_at IS NULL";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("revoke share link failed id={}: {}", id, e.getMessage(), e);
            return false;
        }
    }

    /** Revokes all active links for a given note (e.g., on note deletion or privacy change). */
    public boolean revokeAllForNote(Long noteId) {
        final String sql =
            "UPDATE share_links SET revoked_at = CURRENT_TIMESTAMP " +
            "WHERE note_id = ? AND revoked_at IS NULL";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            log.error("revokeAllForNote failed noteId={}: {}", noteId, e.getMessage(), e);
            return false;
        }
    }

    // --------------------------------------------------------------- helpers

    private ShareLink mapRow(ResultSet rs) throws SQLException {
        ShareLink sl = new ShareLink();
        sl.setId(rs.getLong("id"));
        sl.setNoteId(rs.getLong("note_id"));
        sl.setTokenHash(rs.getString("token_hash"));
        sl.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp revoked = rs.getTimestamp("revoked_at");
        if (revoked != null) sl.setRevokedAt(revoked.toLocalDateTime());
        return sl;
    }
}
