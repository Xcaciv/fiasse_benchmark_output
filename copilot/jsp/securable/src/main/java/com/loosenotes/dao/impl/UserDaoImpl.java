package com.loosenotes.dao.impl;

import com.loosenotes.dao.UserDao;
import com.loosenotes.model.User;
import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** JDBC implementation — all queries use PreparedStatement (Integrity). */
public final class UserDaoImpl implements UserDao {

    private static final Logger log = LoggerFactory.getLogger(UserDaoImpl.class);
    private final DatabaseManager db;

    public UserDaoImpl(DatabaseManager db) {
        this.db = db;
    }

    @Override
    public long insert(User user) {
        final String sql =
            "INSERT INTO users (username, email, password_hash, role) VALUES (?,?,?,?)";
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            log.error("insert user failed", e);
            throw new RuntimeException("Database error inserting user", e);
        }
    }

    @Override
    public Optional<User> findById(long id) {
        return queryOne("SELECT * FROM users WHERE id=?", ps -> ps.setLong(1, id));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return queryOne("SELECT * FROM users WHERE username=?",
                ps -> ps.setString(1, username));
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return queryOne("SELECT * FROM users WHERE email=?",
                ps -> ps.setString(1, email));
    }

    @Override
    public Optional<User> findByResetToken(String token) {
        return queryOne("SELECT * FROM users WHERE reset_token=?",
                ps -> ps.setString(1, token));
    }

    @Override
    public boolean update(User user) {
        final String sql =
            "UPDATE users SET username=?, email=?, role=? WHERE id=?";
        return executeUpdate(sql, ps -> {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getEmail());
            ps.setString(3, user.getRole());
            ps.setLong(4, user.getId());
        });
    }

    @Override
    public boolean updatePassword(long userId, String newHash) {
        return executeUpdate("UPDATE users SET password_hash=? WHERE id=?", ps -> {
            ps.setString(1, newHash);
            ps.setLong(2, userId);
        });
    }

    @Override
    public boolean setResetToken(long userId, String token, String expiresAt) {
        return executeUpdate(
            "UPDATE users SET reset_token=?, reset_token_exp=? WHERE id=?", ps -> {
                ps.setString(1, token);
                ps.setString(2, expiresAt);
                ps.setLong(3, userId);
            });
    }

    @Override
    public boolean clearResetToken(long userId) {
        return executeUpdate(
            "UPDATE users SET reset_token=NULL, reset_token_exp=NULL WHERE id=?",
            ps -> ps.setLong(1, userId));
    }

    @Override
    public List<User> findAll() {
        return queryMany("SELECT * FROM users ORDER BY created_at DESC", ps -> {});
    }

    @Override
    public List<User> searchByUsernameOrEmail(String query) {
        String like = "%" + query + "%";
        return queryMany(
            "SELECT * FROM users WHERE username LIKE ? OR email LIKE ? ORDER BY username",
            ps -> { ps.setString(1, like); ps.setString(2, like); });
    }

    @Override
    public int countAll() {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM users");
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            log.error("countAll users failed", e);
            return 0;
        }
    }

    @Override
    public boolean existsByUsername(String username) {
        return queryCount("SELECT COUNT(*) FROM users WHERE username=?",
                ps -> ps.setString(1, username)) > 0;
    }

    @Override
    public boolean existsByEmail(String email) {
        return queryCount("SELECT COUNT(*) FROM users WHERE email=?",
                ps -> ps.setString(1, email)) > 0;
    }

    @Override
    public int countNotesByUserId(long userId) {
        return queryCount("SELECT COUNT(*) FROM notes WHERE user_id=?",
                ps -> ps.setLong(1, userId));
    }

    // --- Private helpers ---

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement ps) throws SQLException;
    }

    private Optional<User> queryOne(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            log.error("queryOne user failed sql={}", sql, e);
            return Optional.empty();
        }
    }

    private List<User> queryMany(String sql, StatementBinder binder) {
        List<User> results = new ArrayList<>();
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) results.add(mapRow(rs));
            }
        } catch (SQLException e) {
            log.error("queryMany users failed sql={}", sql, e);
        }
        return results;
    }

    private boolean executeUpdate(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            log.error("executeUpdate users failed sql={}", sql, e);
            return false;
        }
    }

    private int queryCount(String sql, StatementBinder binder) {
        try (Connection c = db.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            binder.bind(ps);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            log.error("queryCount failed sql={}", sql, e);
            return 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        String createdStr = rs.getString("created_at");
        LocalDateTime created = createdStr != null
                ? LocalDateTime.parse(createdStr.replace(" ", "T"))
                : LocalDateTime.now();
        return new User(
            rs.getLong("id"),
            rs.getString("username"),
            rs.getString("email"),
            rs.getString("password_hash"),
            rs.getString("role"),
            created
        );
    }
}
