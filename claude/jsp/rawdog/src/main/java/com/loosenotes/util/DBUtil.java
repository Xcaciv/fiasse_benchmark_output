package com.loosenotes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DBUtil {
    private static final Logger logger = LoggerFactory.getLogger(DBUtil.class);

    private static final String DATA_DIR = System.getProperty("user.home") + File.separator + ".loosenotes";
    private static final String DB_PATH = DATA_DIR + File.separator + "loosenotes.db";
    public static final String UPLOAD_DIR = DATA_DIR + File.separator + "uploads";
    private static final String JDBC_URL = "jdbc:sqlite:" + DB_PATH;

    static {
        try {
            new File(DATA_DIR).mkdirs();
            new File(UPLOAD_DIR).mkdirs();
            Class.forName("org.sqlite.JDBC");
            initializeSchema();
            createDefaultAdmin();
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        Connection conn = DriverManager.getConnection(JDBC_URL);
        conn.createStatement().execute("PRAGMA foreign_keys = ON");
        return conn;
    }

    private static void initializeSchema() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL);
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    email TEXT UNIQUE NOT NULL,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL DEFAULT 'USER',
                    created_at TEXT DEFAULT (datetime('now')),
                    reset_token TEXT,
                    reset_token_expiry TEXT
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS notes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    content TEXT NOT NULL,
                    user_id INTEGER NOT NULL,
                    is_public INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT DEFAULT (datetime('now')),
                    updated_at TEXT DEFAULT (datetime('now')),
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attachments (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    note_id INTEGER NOT NULL,
                    original_filename TEXT NOT NULL,
                    stored_filename TEXT NOT NULL,
                    file_size INTEGER NOT NULL DEFAULT 0,
                    created_at TEXT DEFAULT (datetime('now')),
                    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ratings (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    note_id INTEGER NOT NULL,
                    user_id INTEGER NOT NULL,
                    rating_value INTEGER NOT NULL CHECK(rating_value >= 1 AND rating_value <= 5),
                    comment TEXT,
                    created_at TEXT DEFAULT (datetime('now')),
                    updated_at TEXT DEFAULT (datetime('now')),
                    UNIQUE(note_id, user_id),
                    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS share_links (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    note_id INTEGER NOT NULL,
                    token TEXT UNIQUE NOT NULL,
                    created_at TEXT DEFAULT (datetime('now')),
                    FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE
                )
                """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS audit_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id INTEGER,
                    action TEXT NOT NULL,
                    details TEXT,
                    created_at TEXT DEFAULT (datetime('now'))
                )
                """);

            // Indexes
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notes_is_public ON notes(is_public)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_share_links_token ON share_links(token)");

            logger.info("Database schema initialized at: {}", DB_PATH);
        }
    }

    private static void createDefaultAdmin() throws SQLException {
        try (Connection conn = DriverManager.getConnection(JDBC_URL)) {
            var rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'"
            );
            if (rs.getInt(1) == 0) {
                String hash = PasswordUtil.hash("Admin@123");
                var pstmt = conn.prepareStatement(
                    "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, 'ADMIN')"
                );
                pstmt.setString(1, "admin");
                pstmt.setString(2, "admin@loosenotes.local");
                pstmt.setString(3, hash);
                pstmt.executeUpdate();
                logger.info("Default admin account created: admin / Admin@123");
            }
        }
    }
}
