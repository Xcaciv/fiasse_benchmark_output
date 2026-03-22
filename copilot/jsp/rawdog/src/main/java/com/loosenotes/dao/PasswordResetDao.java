package com.loosenotes.dao;

import com.loosenotes.db.ConnectionFactory;
import com.loosenotes.model.PasswordResetToken;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

public class PasswordResetDao extends BaseDao {
    public void createToken(long userId, String token, String expiresAt) {
        String now = now();
        try (Connection connection = ConnectionFactory.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement revoke = connection.prepareStatement("UPDATE password_reset_tokens SET used_at = ? WHERE user_id = ? AND used_at IS NULL");
                 PreparedStatement insert = connection.prepareStatement("INSERT INTO password_reset_tokens(user_id, token, expires_at, used_at, created_at) VALUES (?, ?, ?, NULL, ?)");) {
                revoke.setString(1, now);
                revoke.setLong(2, userId);
                revoke.executeUpdate();

                insert.setLong(1, userId);
                insert.setString(2, token);
                insert.setString(3, expiresAt);
                insert.setString(4, now);
                insert.executeUpdate();
                connection.commit();
            } catch (SQLException ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to create password reset token.", ex);
        }
    }

    public Optional<PasswordResetToken> findUsableByToken(String token) {
        String sql = "SELECT * FROM password_reset_tokens WHERE token = ? AND used_at IS NULL";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, token);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    PasswordResetToken resetToken = map(resultSet);
                    if (resetToken.getExpiresAt() != null && resetToken.getExpiresAt().isAfter(java.time.LocalDateTime.now())) {
                        return Optional.of(resetToken);
                    }
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load password reset token.", ex);
        }
    }

    public void markUsed(String token) {
        String sql = "UPDATE password_reset_tokens SET used_at = ? WHERE token = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, now());
            statement.setString(2, token);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to mark password reset token as used.", ex);
        }
    }

    private PasswordResetToken map(ResultSet resultSet) throws SQLException {
        PasswordResetToken token = new PasswordResetToken();
        token.setId(resultSet.getLong("id"));
        token.setUserId(resultSet.getLong("user_id"));
        token.setToken(resultSet.getString("token"));
        token.setExpiresAt(parseTimestamp(resultSet.getString("expires_at")));
        token.setUsedAt(parseTimestamp(resultSet.getString("used_at")));
        token.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
        return token;
    }
}
