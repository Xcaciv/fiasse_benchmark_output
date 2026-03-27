package com.loosenotes.dao;

import com.loosenotes.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-access object for the {@code users} table.
 * Every method opens and closes its own Connection via try-with-resources.
 * PreparedStatements are used exclusively — no string concatenation in SQL.
 */
public class UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDao.class);

    private final DatabaseManager db = DatabaseManager.getInstance();

    // ------------------------------------------------------------------ reads

    public User findById(Long id) {
        final String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findById failed for id={}: {}", id, e.getMessage(), e);
            return null;
        }
    }

    public User findByUsername(String username) {
        final String sql = "SELECT * FROM users WHERE username = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findByUsername failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public User findByEmail(String email) {
        final String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            log.error("findByEmail failed: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<User> findAll(int page, int pageSize) {
        final String sql = "SELECT * FROM users ORDER BY id LIMIT ? OFFSET ?";
        List<User> users = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, pageSize);
            ps.setInt(2, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("findAll failed: {}", e.getMessage(), e);
        }
        return users;
    }

    public int countAll() {
        final String sql = "SELECT COUNT(*) FROM users";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.error("countAll failed: {}", e.getMessage(), e);
            return 0;
        }
    }

    public List<User> searchByUsernameOrEmail(String query, int page, int pageSize) {
        final String sql =
            "SELECT * FROM users WHERE username LIKE ? OR email LIKE ? " +
            "ORDER BY username LIMIT ? OFFSET ?";
        List<User> users = new ArrayList<>();
        String pattern = "%" + query + "%";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setInt(3, pageSize);
            ps.setInt(4, (page - 1) * pageSize);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) users.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("searchByUsernameOrEmail failed: {}", e.getMessage(), e);
        }
        return users;
    }

    // ----------------------------------------------------------------- writes

    public boolean create(User user) {
        final String sql =
            "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole().name());
            int rows = ps.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) user.setId(keys.getLong(1));
                }
                return true;
            }
        } catch (SQLException e) {
            log.error("create user failed for username={}: {}", user.getUsername(), e.getMessage(), e);
        }
        return false;
    }

    public boolean updateFailedAttempts(Long userId, int attempts, LocalDateTime lockoutUntil) {
        final String sql =
            "UPDATE users SET failed_login_attempts = ?, lockout_until = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, attempts);
            ps.setObject(2, lockoutUntil);   // null-safe via setObject
            ps.setLong(3, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("updateFailedAttempts failed for userId={}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    public boolean updatePassword(Long userId, String passwordHash) {
        final String sql = "UPDATE users SET password_hash = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, passwordHash);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("updatePassword failed for userId={}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    public boolean updateUsername(Long userId, String username) {
        final String sql = "UPDATE users SET username = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("updateUsername failed for userId={}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    public boolean updateEmail(Long userId, String email) {
        final String sql = "UPDATE users SET email = ? WHERE id = ?";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setLong(2, userId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("updateEmail failed for userId={}: {}", userId, e.getMessage(), e);
            return false;
        }
    }

    // --------------------------------------------------------------- helpers

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getLong("id"));
        u.setUsername(rs.getString("username"));
        u.setEmail(rs.getString("email"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(User.Role.valueOf(rs.getString("role")));
        u.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        Timestamp lockout = rs.getTimestamp("lockout_until");
        if (lockout != null) u.setLockoutUntil(lockout.toLocalDateTime());
        u.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        u.setUpdatedAt(rs.getTimestamp("updated_at").toLocalDateTime());
        return u;
    }
}
