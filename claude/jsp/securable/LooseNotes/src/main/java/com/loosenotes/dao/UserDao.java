package com.loosenotes.dao;

import com.loosenotes.model.Role;
import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access layer for User entities.
 * SSEM: Integrity - all queries use PreparedStatement (no SQL injection).
 * SSEM: Resilience - try-with-resources ensures connection cleanup.
 * SSEM: Testability - depends on DataSource interface for mockability.
 */
public class UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDao.class);
    private final DataSource dataSource;

    public UserDao(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /** Inserts a new user and returns the generated ID. */
    public long create(User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
            }
        }
        throw new SQLException("User creation failed: no generated key returned");
    }

    /** Finds a user by primary key. */
    public Optional<User> findById(long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Finds a user by username (case-sensitive). */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Finds a user by email (case-insensitive). */
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapRow(rs));
            }
        }
        return Optional.empty();
    }

    /** Returns all users (admin use only). */
    public List<User> findAll(int page, int pageSize) throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?";
        List<User> users = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        }
        return users;
    }

    /** Searches users by username or email (admin use only). */
    public List<User> search(String query, int pageSize) throws SQLException {
        String sql = "SELECT * FROM users WHERE LOWER(username) LIKE ? OR LOWER(email) LIKE ? "
            + "ORDER BY username LIMIT ?";
        String pattern = "%" + query.toLowerCase() + "%";
        List<User> users = new ArrayList<>();
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        }
        return users;
    }

    /** Updates email and username for profile editing. */
    public void updateProfile(long userId, String username, String email) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, email);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    /** Updates password hash. */
    public void updatePasswordHash(long userId, String newHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    /** Increments failed login count; locks account if threshold reached. */
    public void recordFailedLogin(long userId, int lockThreshold,
                                   LocalDateTime lockUntil) throws SQLException {
        String sql = "UPDATE users SET failed_login_attempts = failed_login_attempts + 1, "
            + "locked_until = CASE WHEN failed_login_attempts + 1 >= ? THEN ? ELSE locked_until END "
            + "WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, lockThreshold);
            ps.setTimestamp(2, Timestamp.valueOf(lockUntil));
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    /** Resets failed login count and removes account lock. */
    public void resetFailedLogins(long userId) throws SQLException {
        String sql = "UPDATE users SET failed_login_attempts = 0, locked_until = NULL WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    /** Counts all users. */
    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        }
        return 0L;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        user.setRole(Role.fromString(rs.getString("role")));
        user.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        user.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        Timestamp locked = rs.getTimestamp("locked_until");
        if (locked != null) user.setLockedUntil(locked.toLocalDateTime());
        return user;
    }
}
