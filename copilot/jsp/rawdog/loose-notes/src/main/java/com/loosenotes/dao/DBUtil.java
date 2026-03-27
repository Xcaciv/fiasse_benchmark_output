package com.loosenotes.dao;

import org.mindrot.jbcrypt.BCrypt;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;

@WebListener
public class DBUtil implements ServletContextListener {

    private static final String DB_DIR = System.getProperty("user.home") + "/loose-notes-data";
    private static final String DB_PATH = DB_DIR + "/loosenotes.db";
    public static final String UPLOADS_DIR = DB_DIR + "/uploads";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        new File(DB_DIR).mkdirs();
        new File(UPLOADS_DIR).mkdirs();
        try (Connection conn = getConnection()) {
            createTables(conn);
            seedAdmin(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {}

    private void createTables(Connection conn) throws SQLException {
        String[] ddl = {
            "CREATE TABLE IF NOT EXISTS users (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  username TEXT NOT NULL UNIQUE," +
            "  email TEXT NOT NULL UNIQUE," +
            "  password_hash TEXT NOT NULL," +
            "  role TEXT NOT NULL DEFAULT 'USER'," +
            "  created_at TEXT NOT NULL" +
            ")",

            "CREATE TABLE IF NOT EXISTS notes (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  user_id INTEGER NOT NULL," +
            "  title TEXT NOT NULL," +
            "  content TEXT NOT NULL," +
            "  visibility TEXT NOT NULL DEFAULT 'PRIVATE'," +
            "  created_at TEXT NOT NULL," +
            "  updated_at TEXT NOT NULL," +
            "  FOREIGN KEY (user_id) REFERENCES users(id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS attachments (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  note_id INTEGER NOT NULL," +
            "  original_filename TEXT NOT NULL," +
            "  stored_filename TEXT NOT NULL," +
            "  file_size INTEGER NOT NULL," +
            "  uploaded_at TEXT NOT NULL," +
            "  FOREIGN KEY (note_id) REFERENCES notes(id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS ratings (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  note_id INTEGER NOT NULL," +
            "  user_id INTEGER NOT NULL," +
            "  rating INTEGER NOT NULL," +
            "  comment TEXT," +
            "  created_at TEXT NOT NULL," +
            "  UNIQUE(note_id, user_id)," +
            "  FOREIGN KEY (note_id) REFERENCES notes(id)," +
            "  FOREIGN KEY (user_id) REFERENCES users(id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS share_links (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  note_id INTEGER NOT NULL UNIQUE," +
            "  token TEXT NOT NULL UNIQUE," +
            "  created_at TEXT NOT NULL," +
            "  FOREIGN KEY (note_id) REFERENCES notes(id)" +
            ")",

            "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
            "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "  user_id INTEGER NOT NULL," +
            "  token TEXT NOT NULL UNIQUE," +
            "  expires_at TEXT NOT NULL," +
            "  used INTEGER NOT NULL DEFAULT 0" +
            ")"
        };

        for (String sql : ddl) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            }
        }
    }

    private void seedAdmin(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE username = 'admin'")) {
            if (rs.next() && rs.getInt(1) > 0) return;
        }

        String hashedPassword = BCrypt.hashpw("admin123", BCrypt.gensalt());
        String sql = "INSERT INTO users (username, email, password_hash, role, created_at) VALUES (?,?,?,?,?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "admin");
            ps.setString(2, "admin@loosenotes.local");
            ps.setString(3, hashedPassword);
            ps.setString(4, "ADMIN");
            ps.setString(5, LocalDateTime.now().toString());
            ps.executeUpdate();
        }
    }
}
