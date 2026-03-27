package com.loosenotes.dao;

import com.loosenotes.model.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserDao {

    public Optional<User> findById(Connection conn, long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByUsername(Connection conn, String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByEmail(Connection conn, String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public long insert(Connection conn, User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole() != null ? user.getRole() : "USER");
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getLong(1);
            }
        }
    }

    public void updateFailedLoginAttempts(Connection conn, long userId, int attempts, LocalDateTime lockoutUntil) throws SQLException {
        String sql = "UPDATE users SET failed_login_attempts = ?, lockout_until = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setObject(2, lockoutUntil != null ? Timestamp.valueOf(lockoutUntil) : null);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    public void updatePassword(Connection conn, long userId, String passwordHash) throws SQLException {
        String sql = "UPDATE users SET password_hash = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public void updateEmail(Connection conn, long userId, String email) throws SQLException {
        String sql = "UPDATE users SET email = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setLong(2, userId);
            ps.executeUpdate();
        }
    }

    public List<User> findAll(Connection conn, int page, int pageSize) throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC LIMIT ? OFFSET ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) users.add(mapRow(rs));
                return users;
            }
        }
    }

    public long countAll(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    public void deleteById(Connection conn, long userId) throws SQLException {
        String sql = "DELETE FROM users WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        u.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        Timestamp lockout = rs.getTimestamp("lockout_until");
        if (lockout != null) u.setLockoutUntil(lockout.toLocalDateTime());
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        u.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return u;
    }
}
