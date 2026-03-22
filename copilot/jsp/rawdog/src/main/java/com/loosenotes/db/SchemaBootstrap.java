package com.loosenotes.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public final class SchemaBootstrap {
    private SchemaBootstrap() {
    }

    public static void initialize() {
        try (Connection connection = ConnectionFactory.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE IF NOT EXISTS users ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "username TEXT NOT NULL UNIQUE,"
                    + "email TEXT NOT NULL UNIQUE,"
                    + "password_hash TEXT NOT NULL,"
                    + "role TEXT NOT NULL,"
                    + "created_at TEXT NOT NULL)");
            statement.execute("CREATE TABLE IF NOT EXISTS notes ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id INTEGER NOT NULL,"
                    + "title TEXT NOT NULL,"
                    + "content TEXT NOT NULL,"
                    + "is_public INTEGER NOT NULL,"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS attachments ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "note_id INTEGER NOT NULL,"
                    + "storage_name TEXT NOT NULL,"
                    + "original_filename TEXT NOT NULL,"
                    + "content_type TEXT,"
                    + "file_size INTEGER NOT NULL,"
                    + "created_at TEXT NOT NULL,"
                    + "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS ratings ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "note_id INTEGER NOT NULL,"
                    + "user_id INTEGER NOT NULL,"
                    + "rating INTEGER NOT NULL,"
                    + "comment TEXT,"
                    + "created_at TEXT NOT NULL,"
                    + "updated_at TEXT NOT NULL,"
                    + "UNIQUE(note_id, user_id),"
                    + "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,"
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS share_links ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "note_id INTEGER NOT NULL,"
                    + "token TEXT NOT NULL UNIQUE,"
                    + "active INTEGER NOT NULL,"
                    + "created_at TEXT NOT NULL,"
                    + "revoked_at TEXT,"
                    + "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS password_reset_tokens ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "user_id INTEGER NOT NULL,"
                    + "token TEXT NOT NULL UNIQUE,"
                    + "expires_at TEXT NOT NULL,"
                    + "used_at TEXT,"
                    + "created_at TEXT NOT NULL,"
                    + "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE)");
            statement.execute("CREATE TABLE IF NOT EXISTS activity_logs ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "actor_user_id INTEGER,"
                    + "action TEXT NOT NULL,"
                    + "details TEXT NOT NULL,"
                    + "created_at TEXT NOT NULL,"
                    + "FOREIGN KEY (actor_user_id) REFERENCES users(id) ON DELETE SET NULL)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_notes_visibility ON notes(is_public)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_share_links_note_id ON share_links(note_id)");
            statement.execute("CREATE INDEX IF NOT EXISTS idx_password_resets_token ON password_reset_tokens(token)");
        } catch (SQLException ex) {
            throw new IllegalStateException("Unable to initialize database schema.", ex);
        }
    }
}
