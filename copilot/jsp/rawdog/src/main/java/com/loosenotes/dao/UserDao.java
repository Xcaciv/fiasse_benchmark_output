package com.loosenotes.dao;

import com.loosenotes.db.ConnectionFactory;
import com.loosenotes.model.AdminUserView;
import com.loosenotes.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao extends BaseDao {
    public User create(String username, String email, String passwordHash, String role) {
        String sql = "INSERT INTO users(username, email, password_hash, role, created_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setString(3, passwordHash);
            statement.setString(4, role);
            statement.setString(5, now());
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    return findById(keys.getLong(1)).orElseThrow();
                }
            }
            throw new IllegalStateException("User creation did not return an id.");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to create user.", ex);
        }
    }

    public Optional<User> findById(long id) {
        return findOne("SELECT * FROM users WHERE id = ?", id);
    }

    public Optional<User> findByUsername(String username) {
        return findOne("SELECT * FROM users WHERE lower(username) = lower(?)", username);
    }

    public Optional<User> findByEmail(String email) {
        return findOne("SELECT * FROM users WHERE lower(email) = lower(?)", email);
    }

    public Optional<User> findByUsernameOrEmail(String identifier) {
        String sql = "SELECT * FROM users WHERE lower(username) = lower(?) OR lower(email) = lower(?)";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, identifier);
            statement.setString(2, identifier);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(map(resultSet));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to find user.", ex);
        }
    }

    public void updateProfile(long id, String username, String email) {
        String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, email);
            statement.setLong(3, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update profile.", ex);
        }
    }

    public void updatePassword(long id, String passwordHash) {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, passwordHash);
            statement.setLong(2, id);
            statement.executeUpdate();
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to update password.", ex);
        }
    }

    public List<AdminUserView> searchUsers(String query) {
        boolean hasQuery = query != null && !query.isBlank();
        String sql = "SELECT u.id, u.username, u.email, u.role, u.created_at, COUNT(n.id) AS note_count "
                + "FROM users u "
                + "LEFT JOIN notes n ON n.user_id = u.id "
                + "WHERE (? = 0 OR lower(u.username) LIKE ? OR lower(u.email) LIKE ?) "
                + "GROUP BY u.id, u.username, u.email, u.role, u.created_at "
                + "ORDER BY u.created_at DESC";
        String like = "%" + (hasQuery ? query.trim().toLowerCase() : "") + "%";
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, hasQuery ? 1 : 0);
            statement.setString(2, like);
            statement.setString(3, like);
            try (ResultSet resultSet = statement.executeQuery()) {
                List<AdminUserView> users = new ArrayList<>();
                while (resultSet.next()) {
                    AdminUserView view = new AdminUserView();
                    view.setId(resultSet.getLong("id"));
                    view.setUsername(resultSet.getString("username"));
                    view.setEmail(resultSet.getString("email"));
                    view.setRole(resultSet.getString("role"));
                    view.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
                    view.setNoteCount(resultSet.getInt("note_count"));
                    users.add(view);
                }
                return users;
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to search users.", ex);
        }
    }

    public List<User> listAll() {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM users ORDER BY username ASC");
             ResultSet resultSet = statement.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (resultSet.next()) {
                users.add(map(resultSet));
            }
            return users;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to load users.", ex);
        }
    }

    public long countUsers() {
        try (Connection connection = ConnectionFactory.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet resultSet = statement.executeQuery()) {
            return resultSet.next() ? resultSet.getLong(1) : 0L;
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to count users.", ex);
        }
    }

    private Optional<User> findOne(String sql, Object parameter) {
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
            throw new IllegalStateException("Unable to query users.", ex);
        }
    }

    private User map(ResultSet resultSet) throws SQLException {
        User user = new User();
        user.setId(resultSet.getLong("id"));
        user.setUsername(resultSet.getString("username"));
        user.setEmail(resultSet.getString("email"));
        user.setPasswordHash(resultSet.getString("password_hash"));
        user.setRole(resultSet.getString("role"));
        user.setCreatedAt(parseTimestamp(resultSet.getString("created_at")));
        return user;
    }
}
