package com.loosenotes.util;

import org.h2.jdbcx.JdbcConnectionPool;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * SSEM: Secure Storage — centralized data-access entry point using an
 * embedded H2 pool with parameterized-only access (enforced at DAO layer).
 */
public class DatabaseManager {

    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class.getName());

    private static final String DB_DIR  = System.getProperty("user.home") + "/.loosenotes";
    private static final String DB_URL  =
            "jdbc:h2:file:" + DB_DIR + "/data;DATABASE_TO_LOWER=TRUE;AUTO_RECONNECT=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASS = "";

    private static JdbcConnectionPool pool;

    private DatabaseManager() {}

    /** Initialise pool and schema once per JVM. Thread-safe. */
    public static synchronized void initialize() {
        if (pool != null) return;

        new File(DB_DIR).mkdirs();
        pool = JdbcConnectionPool.create(DB_URL, DB_USER, DB_PASS);
        pool.setMaxConnections(20);

        try {
            createSchema();
            LOGGER.info("Database initialised successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Database schema initialisation failed", e);
            throw new RuntimeException("Database initialisation failed", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (pool == null) initialize();
        return pool.getConnection();
    }

    public static void shutdown() {
        if (pool != null) {
            pool.dispose();
            pool = null;
        }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Schema DDL
    // ─────────────────────────────────────────────────────────────────────
    private static void createSchema() throws SQLException {
        try (Connection c = pool.getConnection(); Statement s = c.createStatement()) {

            s.execute(
                "CREATE TABLE IF NOT EXISTS users (" +
                "  id            BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  username      VARCHAR(50)  UNIQUE NOT NULL," +
                "  email         VARCHAR(100) UNIQUE NOT NULL," +
                "  password_hash VARCHAR(255) NOT NULL," +
                "  role          VARCHAR(20)  NOT NULL DEFAULT 'USER'," +
                "  created_at    TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  reset_token         VARCHAR(255)," +
                "  reset_token_expiry  TIMESTAMP" +
                ")"
            );

            s.execute(
                "CREATE TABLE IF NOT EXISTS notes (" +
                "  id         BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  user_id    BIGINT       NOT NULL," +
                "  title      VARCHAR(255) NOT NULL," +
                "  content    TEXT         NOT NULL," +
                "  is_public  BOOLEAN      NOT NULL DEFAULT FALSE," +
                "  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")"
            );

            s.execute(
                "CREATE TABLE IF NOT EXISTS attachments (" +
                "  id                BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  note_id           BIGINT       NOT NULL," +
                "  original_filename VARCHAR(255) NOT NULL," +
                "  stored_filename   VARCHAR(255) NOT NULL," +
                "  file_size         BIGINT       NOT NULL," +
                "  uploaded_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE" +
                ")"
            );

            s.execute(
                "CREATE TABLE IF NOT EXISTS ratings (" +
                "  id         BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  note_id    BIGINT NOT NULL," +
                "  user_id    BIGINT NOT NULL," +
                "  rating     INT    NOT NULL CHECK (rating >= 1 AND rating <= 5)," +
                "  comment    TEXT," +
                "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  UNIQUE (note_id, user_id)," +
                "  FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE," +
                "  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")"
            );

            s.execute(
                "CREATE TABLE IF NOT EXISTS share_links (" +
                "  id         BIGINT AUTO_INCREMENT PRIMARY KEY," +
                "  note_id    BIGINT       NOT NULL," +
                "  token      VARCHAR(255) UNIQUE NOT NULL," +
                "  created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "  FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE" +
                ")"
            );

            s.execute("CREATE INDEX IF NOT EXISTS idx_notes_user_id  ON notes(user_id)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_notes_public    ON notes(is_public)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id)");
            s.execute("CREATE INDEX IF NOT EXISTS idx_share_token     ON share_links(token)");
        }
    }
}
