package com.loosenotes.dao.impl;

import com.loosenotes.dao.ShareLinkDao;
import com.loosenotes.model.ShareLink;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Optional;

public final class ShareLinkDaoImpl implements ShareLinkDao {

    private static final Logger log = LoggerFactory.getLogger(ShareLinkDaoImpl.class);
    private final DatabaseManager db;

    public ShareLinkDaoImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public long insert(ShareLink link) {
        final String sql =
            "INSERT OR REPLACE INTO share_links (note_id, token) VALUES (?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, link.getNoteId());
            ps.setString(2, link.getToken());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            log.error("insert share_link failed", e);
            throw new RuntimeException("Database error inserting share link", e);
        }
    }

    @Override
    public Optional<ShareLink> findByNoteId(long noteId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM share_links WHERE note_id=?")) {
            ps.setLong(1, noteId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("findByNoteId share_link failed", e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<ShareLink> findByToken(String token) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT * FROM share_links WHERE token=?")) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("findByToken share_link failed", e);
            return Optional.empty();
        }
    }

    @Override
    public boolean deleteByNoteId(long noteId) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM share_links WHERE note_id=?")) {
            ps.setLong(1, noteId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("deleteByNoteId share_link failed", e);
            return false;
        }
    }

    private ShareLink mapRow(ResultSet rs) throws SQLException {
        String createdStr = rs.getString("created_at");
        LocalDateTime created = createdStr != null
                ? LocalDateTime.parse(createdStr.replace(" ", "T"))
                : LocalDateTime.now();
        return new ShareLink(
            rs.getLong("id"),
            rs.getLong("note_id"),
            rs.getString("token"),
            created
        );
    }
}
