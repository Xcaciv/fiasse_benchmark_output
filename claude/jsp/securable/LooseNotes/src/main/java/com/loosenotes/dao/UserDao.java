package com.loosenotes.dao;

import com.loosenotes.model.Role;
import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the users table.
 *
 * SSEM notes:
 * - Integrity: exclusively uses PreparedStatement – no SQL concatenation.
 * - Confidentiality: never logs passwords or password hashes.
 * - Analyzability: each method has a single responsibility.
 * - Resilience: connections closed via try-with-resources; explicit null checks.
 */
public class UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDao.class);
    private final DatabaseManager db;

    public UserDao(DatabaseManager db) {
        this.db = db;
    }

    /** Finds a user by their ID. Returns empty if not found. */
    public Optional<User> findById(long id) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, role, enabled, "
                + "created_at, updated_at FROM users WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Finds a user by username (case-insensitive). Returns empty if not found. */
    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, role, enabled, "
                + "created_at, updated_at FROM users WHERE LOWER(username) = LOWER(?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /** Finds a user by email address (case-insensitive). */
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, role, enabled, "
                + "created_at, updated_at FROM users WHERE LOWER(email) = LOWER(?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    /**
     * Inserts a new user record. Returns the generated ID.
     * The passwordHash must already be a BCrypt hash before this call.
     */
    public long insert(User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, role, enabled) "
                + "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isEnabled());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) return keys.getLong(1);
                throw new SQLException("No generated key returned for user insert");
            }
        }
    }

    /** Updates a user's mutable profile fields. */
    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET username = ?, email = ?, password_hash = ?, "
                + "role = ?, enabled = ?, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setBoolean(5, user.isEnabled());
            ps.setLong(6, user.getId());
            ps.executeUpdate();
        }
    }

    /** Lists all users (admin use). */
    public List<User> findAll() throws SQLException {
        String sql = "SELECT id, username, email, password_hash, role, enabled, "
                + "created_at, updated_at FROM users ORDER BY created_at DESC";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            List<User> users = new ArrayList<>();
            while (rs.next()) users.add(mapRow(rs));
            return users;
        }
    }

    /**
     * Searches users by username or email (admin use).
     * Uses parameterized LIKE – '%' is part of the bind value.
     */
    public List<User> search(String query) throws SQLException {
        String sql = "SELECT id, username, email, password_hash, role, enabled, "
                + "created_at, updated_at FROM users "
                + "WHERE LOWER(username) LIKE ? OR LOWER(email) LIKE ? "
                + "ORDER BY username";
        String pattern = "%" + query.toLowerCase() + "%";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                List<User> users = new ArrayList<>();
                while (rs.next()) users.add(mapRow(rs));
                return users;
            }
        }
    }

    /** Returns total user count (admin dashboard). */
    public int countAll() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(Role.valueOf(rs.getString("role")));
        u.setEnabled(rs.getBoolean("enabled"));
        u.setCreatedAt(rs.getTimestamp("created_at").toInstant());
        u.setUpdatedAt(rs.getTimestamp("updated_at").toInstant());
        return u;
    }
}
