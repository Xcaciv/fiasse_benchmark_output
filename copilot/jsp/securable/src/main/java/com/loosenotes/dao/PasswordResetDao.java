package com.loosenotes.dao;

import com.loosenotes.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.OptionalLong;

public class PasswordResetDao {
    public void createToken(long userId, String tokenHash) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                try (PreparedStatement invalidate = connection.prepareStatement(
                    "UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE user_id = ? AND used_at IS NULL")) {
                    invalidate.setLong(1, userId);
                    invalidate.executeUpdate();
                }
                try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO password_reset_tokens (user_id, token_hash, expires_at) VALUES (?, ?, datetime('now', '+1 hour'))")) {
                    insert.setLong(1, userId);
                    insert.setString(2, tokenHash);
                    insert.executeUpdate();
                }
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    public boolean tokenIsUsable(String tokenHash) throws SQLException {
        String sql = "SELECT COUNT(*) FROM password_reset_tokens WHERE token_hash = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tokenHash);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    public OptionalLong consumeToken(String tokenHash) throws SQLException {
        try (Connection connection = Database.getConnection()) {
            connection.setAutoCommit(false);
            try {
                Long tokenId = null;
                Long userId = null;
                try (PreparedStatement select = connection.prepareStatement(
                    "SELECT id, user_id FROM password_reset_tokens WHERE token_hash = ? AND used_at IS NULL AND expires_at > CURRENT_TIMESTAMP")) {
                    select.setString(1, tokenHash);
                    try (ResultSet rs = select.executeQuery()) {
                        if (rs.next()) {
                            tokenId = rs.getLong("id");
                            userId = rs.getLong("user_id");
                        }
                    }
                }
                if (tokenId == null || userId == null) {
                    connection.rollback();
                    return OptionalLong.empty();
                }
                try (PreparedStatement update = connection.prepareStatement(
                    "UPDATE password_reset_tokens SET used_at = CURRENT_TIMESTAMP WHERE id = ?")) {
                    update.setLong(1, tokenId);
                    update.executeUpdate();
                }
                connection.commit();
                return OptionalLong.of(userId);
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }
}
