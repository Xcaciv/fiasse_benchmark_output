package com.loosenotes.dao;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DatabaseUtil {

    private static final String DB_PATH;

    static {
        DB_PATH = System.getProperty("user.home") + "/loose-notes-data/notes.db";
        try {
            Class.forName("org.sqlite.JDBC");
            File dbFile = new File(DB_PATH);
            dbFile.getParentFile().mkdirs();
            initSchema();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    private static void initSchema() throws Exception {
        try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  username TEXT NOT NULL UNIQUE," +
                "  email TEXT NOT NULL UNIQUE," +
                "  password_hash TEXT NOT NULL," +
                "  is_admin INTEGER NOT NULL DEFAULT 0," +
                "  created_at TEXT NOT NULL" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS notes (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  user_id INTEGER NOT NULL," +
                "  title TEXT NOT NULL," +
                "  content TEXT NOT NULL," +
                "  is_public INTEGER NOT NULL DEFAULT 0," +
                "  created_at TEXT NOT NULL," +
                "  updated_at TEXT NOT NULL," +
                "  FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS attachments (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  note_id INTEGER NOT NULL," +
                "  original_filename TEXT NOT NULL," +
                "  stored_filename TEXT NOT NULL," +
                "  uploaded_at TEXT NOT NULL," +
                "  FOREIGN KEY (note_id) REFERENCES notes(id)" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS ratings (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  note_id INTEGER NOT NULL," +
                "  user_id INTEGER NOT NULL," +
                "  rating INTEGER NOT NULL CHECK(rating BETWEEN 1 AND 5)," +
                "  comment TEXT," +
                "  created_at TEXT NOT NULL," +
                "  UNIQUE(note_id, user_id)," +
                "  FOREIGN KEY (note_id) REFERENCES notes(id)," +
                "  FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS share_links (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  note_id INTEGER NOT NULL UNIQUE," +
                "  token TEXT NOT NULL UNIQUE," +
                "  created_at TEXT NOT NULL," +
                "  FOREIGN KEY (note_id) REFERENCES notes(id)" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  user_id INTEGER NOT NULL," +
                "  token TEXT NOT NULL UNIQUE," +
                "  expires_at TEXT NOT NULL," +
                "  used INTEGER NOT NULL DEFAULT 0," +
                "  FOREIGN KEY (user_id) REFERENCES users(id)" +
                ")"
            );
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS activity_log (" +
                "  id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "  user_id INTEGER," +
                "  action TEXT NOT NULL," +
                "  detail TEXT," +
                "  created_at TEXT NOT NULL" +
                ")"
            );
        }
    }
}
