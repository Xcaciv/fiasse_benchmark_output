package com.loosenotes.dao;

import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the users table.
 * All queries use PreparedStatements; no SQL string concatenation.
 */
public class UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDao.class);
    private final DatabaseManager dbManager;

    public UserDao(DatabaseManager dbManager) {
        this.dbManager = dbManager;
    }

    /** Inserts a new user and sets the generated id back on the object. */
    public void save(User user) throws SQLException {
        String sql = "INSERT INTO users (username, email, password_hash, role, created_at) VALUES (?,?,?,?,?)";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            ps.setLong(5, user.getCreatedAt());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    user.setId(keys.getLong(1));
                }
            }
        }
    }

    public Optional<User> findById(long id) throws SQLException {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<User> findByResetToken(String token) throws SQLException {
        String sql = "SELECT * FROM users WHERE reset_token = ?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public void update(User user) throws SQLException {
        String sql = "UPDATE users SET username=?, email=?, password_hash=? WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setLong(4, user.getId());
            ps.executeUpdate();
        }
    }

    public void updateResetToken(long userId, String token, long expiry) throws SQLException {
        String sql = "UPDATE users SET reset_token=?, reset_token_expiry=? WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, token);
            ps.setLong(2, expiry);
            ps.setLong(3, userId);
            ps.executeUpdate();
        }
    }

    public void clearResetToken(long userId) throws SQLException {
        String sql = "UPDATE users SET reset_token=NULL, reset_token_expiry=NULL WHERE id=?";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.executeUpdate();
        }
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT * FROM users ORDER BY created_at DESC";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return mapRows(rs);
        }
    }

    public long count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM users";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    /** Admin user search — matches username OR email using parameterised LIKE. */
    public List<User> findByUsernameOrEmail(String query) throws SQLException {
        String sql = "SELECT * FROM users WHERE username LIKE ? OR email LIKE ? ORDER BY username";
        String pattern = "%" + query + "%";
        try (Connection conn = dbManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                return mapRows(rs);
            }
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getLong("id"));
        user.setUsername(rs.getString("username"));
        user.setEmail(rs.getString("email"));
        user.setPasswordHash(rs.getString("password_hash"));
        String roleStr = rs.getString("role");
        user.setRole(User.Role.valueOf(roleStr != null ? roleStr : "USER"));
        user.setResetToken(rs.getString("reset_token"));
        long expiry = rs.getLong("reset_token_expiry");
        user.setResetTokenExpiry(rs.wasNull() ? null : expiry);
        user.setCreatedAt(rs.getLong("created_at"));
        return user;
    }

    private List<User> mapRows(ResultSet rs) throws SQLException {
        List<User> users = new ArrayList<>();
        while (rs.next()) {
            users.add(mapRow(rs));
        }
        return users;
    }
}
