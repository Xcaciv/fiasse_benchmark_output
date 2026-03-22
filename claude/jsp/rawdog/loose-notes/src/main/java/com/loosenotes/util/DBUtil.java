package com.loosenotes.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;

public class DBUtil {

    private static final Logger logger = Logger.getLogger(DBUtil.class.getName());
    private static String dbPath;
    private static boolean initialized = false;

    static {
        String home = System.getProperty("user.home");
        File dir = new File(home + "/.loosenotes");
        if (!dir.exists()) {
            dir.mkdirs();
        }
        dbPath = home + "/.loosenotes/loosenotes.db";
        try {
            Class.forName("org.sqlite.JDBC");
            initSchema();
        } catch (ClassNotFoundException e) {
            logger.log(Level.SEVERE, "SQLite JDBC driver not found", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + dbPath);
    }

    private static void initSchema() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("PRAGMA foreign_keys = ON");

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT UNIQUE NOT NULL," +
                "email TEXT UNIQUE NOT NULL," +
                "password_hash TEXT NOT NULL," +
                "role TEXT NOT NULL DEFAULT 'USER'," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS notes (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "title TEXT NOT NULL," +
                "content TEXT NOT NULL," +
                "is_public INTEGER NOT NULL DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS attachments (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "note_id INTEGER NOT NULL," +
                "original_filename TEXT NOT NULL," +
                "stored_filename TEXT NOT NULL," +
                "file_size INTEGER NOT NULL," +
                "content_type TEXT," +
                "uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS ratings (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "note_id INTEGER NOT NULL," +
                "user_id INTEGER NOT NULL," +
                "rating INTEGER NOT NULL," +
                "comment TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE(note_id, user_id)," +
                "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS share_links (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "note_id INTEGER NOT NULL UNIQUE," +
                "token TEXT UNIQUE NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "token TEXT UNIQUE NOT NULL," +
                "expires_at TIMESTAMP NOT NULL," +
                "used INTEGER NOT NULL DEFAULT 0," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS activity_log (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER," +
                "action TEXT NOT NULL," +
                "details TEXT," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");

            // Create default admin user if no users exist
            createDefaultAdmin(conn);

            initialized = true;
            logger.info("Database schema initialized successfully at: " + dbPath);

        } catch (SQLException e) {
            logger.log(Level.SEVERE, "Failed to initialize database schema", e);
        }
    }

    private static void createDefaultAdmin(Connection conn) throws SQLException {
        try (Statement checkStmt = conn.createStatement()) {
            var rs = checkStmt.executeQuery("SELECT COUNT(*) FROM users");
            if (rs.next() && rs.getInt(1) == 0) {
                String adminHash = PasswordUtil.hashPassword("admin123");
                try (var pstmt = conn.prepareStatement(
                        "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)")) {
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, "admin@loosenotes.local");
                    pstmt.setString(3, adminHash);
                    pstmt.setString(4, "ADMIN");
                    pstmt.executeUpdate();
                    logger.info("Default admin user created (username: admin, password: admin123)");
                }
            }
        }
    }
}
