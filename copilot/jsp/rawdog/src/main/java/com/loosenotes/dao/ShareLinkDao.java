package com.loosenotes.dao;

import com.loosenotes.db.ConnectionFactory;
import com.loosenotes.model.ShareLink;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class ShareLinkDao extends BaseDao {
    public ShareLink create(long noteId, String token) {
        String sql = "INSERT INTO share_links(note_id, token, active, created_at, revoked_at) VALUES (?, ?, 1, ?, NULL)";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setLong(1, noteId);
            statement.setString(2, token);
            statement.setString(3, now());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
            throw new IllegalStateException("Share link creation did not return an id.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to create share link.", ex);
        }
    }

    public Optional<ShareLink> findById(long id) {
        return queryOne("SELECT * FROM share_links WHERE id = ?", id);
    }

    public Optional<ShareLink> findActiveByNoteId(long noteId) {
        return queryOne("SELECT * FROM share_links WHERE note_id = ? AND active = 1 ORDER BY created_at DESC LIMIT 1", noteId);
    }

    public Optional<ShareLink> findByToken(String token) {
        return queryOne("SELECT * FROM share_links WHERE token = ? AND active = 1", token);
    }

    public void revokeByNoteId(long noteId) {
        String sql = "UPDATE share_links SET active = 0, revoked_at = ? WHERE note_id = ? AND active = 1";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, now());
            statement.setLong(2, noteId);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to revoke share links.", ex);
        }
    }

    private Optional<ShareLink> queryOne(String sql, Object parameter) {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setObject(1, parameter);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load share link.", ex);
        }
    }

    private ShareLink map(ResultSet resultSet) throws SQLException {
        ShareLink shareLink = new ShareLink();
        shareLink.setId(resultSet.getLong("id"));
        shareLink.setNoteId(resultSet.getLong("note_id"));
        shareLink.setToken(resultSet.getString("token"));
        shareLink.setActive(resultSet.getInt("active") == 1);
        shareLink.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
        shareLink.setRevokedAt(parseTimestamp(resultSet.getString("revoked_at")));
        return shareLink;
    }
}
