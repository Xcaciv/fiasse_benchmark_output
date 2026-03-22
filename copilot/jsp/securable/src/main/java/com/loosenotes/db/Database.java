package com.loosenotes.db;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static String jdbcUrl;

    private Database() {
    }

    public static synchronized void initialize(Path databaseFile) throws Exception {
        if (jdbcUrl != null) {
            return;
        }
        Files.createDirectories(databaseFile.getParent());
        Class.forName("org.sqlite.JDBC");
        jdbcUrl = "jdbc:sqlite:" + databaseFile.toAbsolutePath();
        createSchema();
    }

    public static Connection getConnection() throws SQLException {
        if (jdbcUrl == null) {
            throw new IllegalStateException("Database has not been initialized.");
        }
        Connection connection = DriverManager.getConnection(jdbcUrl);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
            statement.execute("PRAGMA busy_timeout = 5000");
        }
        return connection;
    }

    private static void createSchema() throws SQLException {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(
                "CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "username TEXT NOT NULL UNIQUE, "
                    + "email TEXT NOT NULL UNIQUE, "
                    + "password_hash TEXT NOT NULL, "
                    + "password_salt TEXT NOT NULL, "
                    + "role TEXT NOT NULL DEFAULT 'USER', "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP)"
            );
            statement.execute(
                "CREATE TABLE IF NOT EXISTS notes ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "title TEXT NOT NULL, "
                    + "content TEXT NOT NULL, "
                    + "is_public INTEGER NOT NULL DEFAULT 0, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)"
            );
            statement.execute(
                "CREATE TABLE IF NOT EXISTS attachments ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "note_id INTEGER NOT NULL, "
                    + "stored_name TEXT NOT NULL, "
                    + "original_name TEXT NOT NULL, "
                    + "content_type TEXT NOT NULL, "
                    + "size_bytes INTEGER NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE)"
            );
            statement.execute(
                "CREATE TABLE IF NOT EXISTS ratings ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "note_id INTEGER NOT NULL, "
                    + "user_id INTEGER NOT NULL, "
                    + "rating_value INTEGER NOT NULL CHECK (rating_value BETWEEN 1 AND 5), "
                    + "comment TEXT, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "UNIQUE (note_id, user_id), "
                    + "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)"
            );
            statement.execute(
                "CREATE TABLE IF NOT EXISTS share_links ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "note_id INTEGER NOT NULL, "
                    + "token_hash TEXT NOT NULL UNIQUE, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "revoked_at TEXT, "
                    + "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE)"
            );
            statement.execute(
                "CREATE TABLE IF NOT EXISTS password_reset_tokens ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "user_id INTEGER NOT NULL, "
                    + "token_hash TEXT NOT NULL UNIQUE, "
                    + "expires_at TEXT NOT NULL, "
                    + "used_at TEXT, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)"
            );
            statement.execute(
                "CREATE TABLE IF NOT EXISTS activity_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "actor_user_id INTEGER, "
                    + "action_type TEXT NOT NULL, "
                    + "details TEXT NOT NULL, "
                    + "created_at TEXT NOT NULL DEFAULT CURRENT_TIMESTAMP, "
                    + "FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL)"
            );
            statement.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_notes_public_updated ON notes(is_public, updated_at)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_share_links_note_id ON share_links(note_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_password_reset_user_id ON password_reset_tokens(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_activity_logs_created_at ON activity_logs(created_at)");
        }
    }
}
