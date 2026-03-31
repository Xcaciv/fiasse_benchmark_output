package com.loosenotes.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseUtil {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseUtil.class);
    private static final String DB_URL = "jdbc:h2:./loosenotes;AUTO_SERVER=TRUE";
    private static final String DB_USER = "sa";
    private static final String DB_PASSWORD = "";
    
    static {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            logger.error("Failed to load H2 database driver", e);
        }
    }
    
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
    }
    
    public static void initializeDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "username VARCHAR(100) NOT NULL UNIQUE, " +
                "email VARCHAR(255) NOT NULL UNIQUE, " +
                "password_hash VARCHAR(255) NOT NULL, " +
                "role VARCHAR(20) NOT NULL DEFAULT 'USER', " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_username ON users(username)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_users_email ON users(email)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS notes (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "title VARCHAR(255) NOT NULL, " +
                "content TEXT NOT NULL, " +
                "is_public BOOLEAN DEFAULT FALSE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notes_user_id ON notes(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_notes_public ON notes(is_public)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS attachments (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "note_id BIGINT NOT NULL, " +
                "original_filename VARCHAR(255) NOT NULL, " +
                "stored_filename VARCHAR(255) NOT NULL UNIQUE, " +
                "content_type VARCHAR(100), " +
                "file_size BIGINT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE" +
                ")");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_attachments_note_id ON attachments(note_id)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS ratings (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "note_id BIGINT NOT NULL, " +
                "user_id BIGINT NOT NULL, " +
                "value INT NOT NULL CHECK (value BETWEEN 1 AND 5), " +
                "comment TEXT, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE, " +
                "UNIQUE(note_id, user_id)" +
                ")");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ratings_note_id ON ratings(note_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_ratings_user_id ON ratings(user_id)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS share_links (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "note_id BIGINT NOT NULL, " +
                "token VARCHAR(255) NOT NULL UNIQUE, " +
                "active BOOLEAN DEFAULT TRUE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "expires_at TIMESTAMP, " +
                "FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE" +
                ")");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_share_links_token ON share_links(token)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_share_links_note_id ON share_links(note_id)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS password_reset_tokens (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT NOT NULL, " +
                "token VARCHAR(255) NOT NULL UNIQUE, " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "expires_at TIMESTAMP NOT NULL, " +
                "used BOOLEAN DEFAULT FALSE, " +
                "FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE" +
                ")");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_password_reset_tokens_token ON password_reset_tokens(token)");
            
            stmt.execute("CREATE TABLE IF NOT EXISTS activity_log (" +
                "id BIGINT AUTO_INCREMENT PRIMARY KEY, " +
                "user_id BIGINT, " +
                "action VARCHAR(100) NOT NULL, " +
                "details TEXT, " +
                "ip_address VARCHAR(45), " +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_activity_log_user_id ON activity_log(user_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_activity_log_created_at ON activity_log(created_at)");
            
            logger.info("Database initialized successfully");
            
        } catch (SQLException e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
}
