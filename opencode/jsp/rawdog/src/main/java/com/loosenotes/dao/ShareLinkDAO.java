package com.loosenotes.dao;

import com.loosenotes.model.ShareLink;
import java.sql.*;
import java.util.Optional;

public class ShareLinkDAO {

    public boolean createShareLink(ShareLink shareLink) {
        String sql = "INSERT INTO share_links (note_id, share_token, is_active, expires_at) VALUES (?, ?, ?, ?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, shareLink.getNoteId());
            stmt.setString(2, shareLink.getShareToken());
            stmt.setBoolean(3, shareLink.isActive());
            if (shareLink.getExpiresAt() != null) {
                stmt.setTimestamp(4, Timestamp.valueOf(shareLink.getExpiresAt()));
            } else {
                stmt.setNull(4, Types.TIMESTAMP);
            }
            int affected = stmt.executeUpdate();
            if (affected > 0) {
                ResultSet rs = stmt.getGeneratedKeys();
                if (rs.next()) {
                    shareLink.setId(rs.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public Optional<ShareLink> findByToken(String token) {
        String sql = "SELECT * FROM share_links WHERE share_token = ? AND is_active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, token);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToShareLink(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<ShareLink> findByNoteId(Long noteId) {
        String sql = "SELECT * FROM share_links WHERE note_id = ? AND is_active = TRUE";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(mapResultSetToShareLink(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean updateToken(Long noteId, String newToken) {
        String sql = "UPDATE share_links SET share_token = ? WHERE note_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newToken);
            stmt.setLong(2, noteId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deactivate(Long id) {
        String sql = "UPDATE share_links SET is_active = FALSE WHERE id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deactivateByNoteId(Long noteId) {
        String sql = "UPDATE share_links SET is_active = FALSE WHERE note_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean deleteByNoteId(Long noteId) {
        String sql = "DELETE FROM share_links WHERE note_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, noteId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private ShareLink mapResultSetToShareLink(ResultSet rs) throws SQLException {
        ShareLink shareLink = new ShareLink();
        shareLink.setId(rs.getLong("id"));
        shareLink.setNoteId(rs.getLong("note_id"));
        shareLink.setShareToken(rs.getString("share_token"));
        shareLink.setActive(rs.getBoolean("is_active"));
        shareLink.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        if (expiresAt != null) {
            shareLink.setExpiresAt(expiresAt.toLocalDateTime());
        }
        return shareLink;
    }
}
