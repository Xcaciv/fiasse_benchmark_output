package com.loosenotes.dao;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {
    private static String driver;
    private static String url;
    private static String username;
    private static String password;
    private static boolean initialized = false;

    public static void init() {
        if (initialized) return;
        
        try (InputStream input = DBConnection.class.getClassLoader().getResourceAsStream("database.properties")) {
            Properties prop = new Properties();
            if (input == null) {
                driver = "com.mysql.cj.jdbc.Driver";
                url = "jdbc:mysql://localhost:3306/loose_notes?useSSL=false&serverTimezone=UTC";
                username = "root";
                password = "password";
            } else {
                prop.load(input);
                driver = prop.getProperty("db.driver", "com.mysql.cj.jdbc.Driver");
                url = prop.getProperty("db.url", "jdbc:mysql://localhost:3306/loose_notes?useSSL=false&serverTimezone=UTC");
                username = prop.getProperty("db.username", "root");
                password = prop.getProperty("db.password", "password");
            }
            Class.forName(driver);
            initialized = true;
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to initialize database connection", e);
        }
    }

    public static Connection getConnection() throws SQLException {
        init();
        return DriverManager.getConnection(url, username, password);
    }

    public static void initializeDatabase() {
        String createUsersTable = """
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                username VARCHAR(50) NOT NULL UNIQUE,
                email VARCHAR(100) NOT NULL UNIQUE,
                password_hash VARCHAR(255) NOT NULL,
                role VARCHAR(20) NOT NULL DEFAULT 'USER',
                active BOOLEAN DEFAULT TRUE,
                reset_token VARCHAR(255),
                reset_token_expiry DATETIME,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                INDEX idx_username (username),
                INDEX idx_email (email)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

        String createNotesTable = """
            CREATE TABLE IF NOT EXISTS notes (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT NOT NULL,
                title VARCHAR(255) NOT NULL,
                content TEXT,
                is_public BOOLEAN DEFAULT FALSE,
                share_token VARCHAR(64),
                share_enabled BOOLEAN DEFAULT FALSE,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                INDEX idx_user_id (user_id),
                INDEX idx_title (title),
                INDEX idx_is_public (is_public),
                FULLTEXT INDEX idx_search (title, content)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

        String createAttachmentsTable = """
            CREATE TABLE IF NOT EXISTS attachments (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                note_id BIGINT NOT NULL,
                original_filename VARCHAR(255) NOT NULL,
                stored_filename VARCHAR(255) NOT NULL,
                file_path VARCHAR(500) NOT NULL,
                file_size BIGINT NOT NULL,
                content_type VARCHAR(100),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
                INDEX idx_note_id (note_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

        String createRatingsTable = """
            CREATE TABLE IF NOT EXISTS ratings (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                note_id BIGINT NOT NULL,
                user_id BIGINT NOT NULL,
                rating INT NOT NULL CHECK (rating >= 1 AND rating <= 5),
                comment TEXT,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                UNIQUE KEY unique_rating (note_id, user_id),
                INDEX idx_note_id (note_id),
                INDEX idx_user_id (user_id)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

        String createShareLinksTable = """
            CREATE TABLE IF NOT EXISTS share_links (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                note_id BIGINT NOT NULL,
                share_token VARCHAR(64) NOT NULL UNIQUE,
                is_active BOOLEAN DEFAULT TRUE,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                expires_at DATETIME,
                FOREIGN KEY (note_id) REFERENCES notes(id) ON DELETE CASCADE,
                INDEX idx_share_token (share_token)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

        String createActivityLogTable = """
            CREATE TABLE IF NOT EXISTS activity_log (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                user_id BIGINT,
                action VARCHAR(50) NOT NULL,
                entity_type VARCHAR(50),
                entity_id BIGINT,
                details TEXT,
                ip_address VARCHAR(45),
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                INDEX idx_user_id (user_id),
                INDEX idx_action (action),
                INDEX idx_created_at (created_at)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;

        try (Connection conn = getConnection()) {
            conn.createStatement().execute(createUsersTable);
            conn.createStatement().execute(createNotesTable);
            conn.createStatement().execute(createAttachmentsTable);
            conn.createStatement().execute(createRatingsTable);
            conn.createStatement().execute(createShareLinksTable);
            conn.createStatement().execute(createActivityLogTable);
            
            createAdminUser(conn);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database tables", e);
        }
    }

    private static void createAdminUser(Connection conn) throws SQLException {
        String checkAdmin = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
        var rs = conn.createStatement().executeQuery(checkAdmin);
        rs.next();
        if (rs.getInt(1) == 0) {
            String insertAdmin = """
                INSERT INTO users (username, email, password_hash, role)
                VALUES ('admin', 'admin@loosenotes.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3.rsQ0e2Jxqy3V0e3xdy', 'ADMIN')
                """;
            conn.createStatement().execute(insertAdmin);
        }
    }
}
