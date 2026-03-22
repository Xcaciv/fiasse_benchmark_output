package com.loosenotes.dao;

import com.loosenotes.db.Database;
import com.loosenotes.model.User;
import com.loosenotes.model.UserListItem;
import com.loosenotes.security.SecurityUtil;
import com.loosenotes.util.AppUtil;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {
    public Optional<User> findById(long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE lower(username) = lower(?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AppUtil.trimToEmpty(username));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE lower(email) = lower(?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AppUtil.trimToEmpty(email));
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapUser(rs));
                }
            }
        }
        return Optional.empty();
    }

    public long createUser(String username, String email, String passwordHash, String passwordSalt, String role) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, password_salt, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, AppUtil.trimToEmpty(username));
            statement.setString(2, AppUtil.trimToEmpty(email));
            statement.setString(3, passwordHash);
            statement.setString(4, passwordSalt);
            statement.setString(5, role);
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        throw new SQLException("Failed to create user record.");
    }

    public boolean createInitialAdminIfMissing(String username, String email, String password) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        try (Connection connection = Database.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery(sql)) {
            if (rs.next() && rs.getLong(1) > 0) {
                return false;
            }
        }
        SecurityUtil.PasswordHash hash = SecurityUtil.hashPassword(password);
        createUser(username, email, hash.getHashBase64(), hash.getSaltBase64(), "ADMIN");
        return true;
    }

    public boolean usernameExists(String username, Long excludeUserId) throws SQLException {
        String sql = excludeUserId == null
            ? "SELECT COUNT(*) FROM users WHERE lower(username) = lower(?)"
            : "SELECT COUNT(*) FROM users WHERE lower(username) = lower(?) AND id <> ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AppUtil.trimToEmpty(username));
            if (excludeUserId != null) {
                statement.setLong(2, excludeUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    public boolean emailExists(String email, Long excludeUserId) throws SQLException {
        String sql = excludeUserId == null
            ? "SELECT COUNT(*) FROM users WHERE lower(email) = lower(?)"
            : "SELECT COUNT(*) FROM users WHERE lower(email) = lower(?) AND id <> ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AppUtil.trimToEmpty(email));
            if (excludeUserId != null) {
                statement.setLong(2, excludeUserId);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() && rs.getLong(1) > 0;
            }
        }
    }

    public void updateProfile(long userId, String username, String email) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, AppUtil.trimToEmpty(username));
            statement.setString(2, AppUtil.trimToEmpty(email));
            statement.setLong(3, userId);
            statement.executeUpdate();
        }
    }

    public void updatePassword(long userId, String passwordHash, String passwordSalt) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, password_salt = ? WHERE id = ?";
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setString(2, passwordSalt);
            statement.setLong(3, userId);
            statement.executeUpdate();
        }
    }

    public long countUsers() throws SQLException {
        try (Connection connection = Database.getConnection(); Statement statement = connection.createStatement(); ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    public List<UserListItem> listUsers(String query) throws SQLException {
        String sql =
            "SELECT u.id, u.username, u.email, u.role, u.created_at, COUNT(n.id) AS note_count "
                + "FROM users u "
                + "LEFT JOIN notes n ON n.user_id = u.id "
                + "WHERE (? = '' OR lower(u.username) LIKE ? OR lower(u.email) LIKE ?) "
                + "GROUP BY u.id, u.username, u.email, u.role, u.created_at "
                + "ORDER BY u.created_at DESC, u.id DESC";
        String clean = AppUtil.trimToEmpty(query);
        String like = AppUtil.likeValue(clean);
        List<UserListItem> users = new ArrayList<>();
        try (Connection connection = Database.getConnection(); PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, clean);
            statement.setString(2, like);
            statement.setString(3, like);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    users.add(new UserListItem(
                        rs.getLong("id"),
                        rs.getString("username"),
                        rs.getString("email"),
                        rs.getString("role"),
                        rs.getString("created_at"),
                        rs.getInt("note_count")
                    ));
                }
            }
        }
        return users;
    }

    public List<UserListItem> listAssignableUsers() throws SQLException {
        return listUsers("");
    }

    private User mapUser(ResultSet rs) throws SQLException {
        return new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("role"),
            rs.getString("password_hash"),
            rs.getString("password_salt"),
            rs.getString("created_at")
        );
    }
}
