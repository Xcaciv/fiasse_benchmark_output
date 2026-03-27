package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the share_links table.
 */
public class ShareLinkDao {

    private static final Logger log = LoggerFactory.getLogger(ShareLinkDao.class);
    private final DatabaseManager dbManager;

    public ShareLinkDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    public void save(ShareLink shareLink) throws SQLException {
        String sql = "INSERT INTO share_links (note_id, token, created_at) VALUES (?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, shareLink.getNoteId());
            ps.setString(2, shareLink.getToken());
            ps.setLong(3, shareLink.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    shareLink.setId(keys.getLong(1));
                }
            }
        }
    }

    public List<ShareLink> findByNoteId(long noteId) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE note_id = ? ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    /** Trust boundary: token is bound as parameter, never concatenated. */
    public Optional<ShareLink> findByToken(String token) throws SQLException {
        String sql = "SELECT * FROM share_links WHERE token = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public void deleteByNoteId(long noteId) throws SQLException {
        String sql = "DELETE FROM share_links WHERE note_id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, noteId);
            ps.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM share_links WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        }
    }

    private ShareLink mapRow(ResultSet rs) throws SQLException {
        ShareLink sl = new ShareLink();
        sl.setId(rs.getLong("id"));
        sl.setNoteId(rs.getLong("note_id"));
        sl.setToken(rs.getString("token"));
        sl.setCreatedAt(rs.getLong("created_at"));
        return sl;
    }

    private List<ShareLink> mapRows(ResultSet rs) throws SQLException {
        List<ShareLink> list = new ArrayList<>();
        while (rs.next()) {
            list.add(mapRow(rs));
        }
        return list;
    }
}
