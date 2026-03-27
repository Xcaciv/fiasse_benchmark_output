package com.loosenotes.dao;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseUtil {
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());
    private static final String DATA_DIR;
    private static final String DB_PATH;
    private static boolean initialized = false;

    static {
        String home = System.getProperty("user.home");
        DATA_DIR = home + "/loosenotes-data";
        DB_PATH = DATA_DIR + "/loosenotes.db";
    }

    private DatabaseUtil() {}

    public static synchronized Connection getConnection() throws SQLException {
        if (!initialized) {
            initDatabase();
        }
        return DriverManager.getConnection("jdbc:sqlite:" + DB_PATH);
    }

    public static String getDataDir() {
        return DATA_DIR;
    }

    public static String getUploadDir() {
        return DATA_DIR + "/uploads";
    }

    private static void initDatabase() {
        try {
            // Create data directory if it doesn't exist
            Path dataPath = Paths.get(DATA_DIR);
            if (!Files.exists(dataPath)) {
                Files.createDirectories(dataPath);
            }
            Path uploadsPath = Paths.get(DATA_DIR + "/uploads");
            if (!Files.exists(uploadsPath)) {
                Files.createDirectories(uploadsPath);
            }

            // Load SQLite driver
            Class.forName("org.sqlite.JDBC");

            // Run schema
            try (Connection conn = DriverManager.getConnection("jdbc:sqlite:" + DB_PATH)) {
                runSchema(conn);
                seedData(conn);
            }
            initialized = true;
            LOGGER.info("Database initialized at: " + DB_PATH);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static void runSchema(Connection conn) throws Exception {
        InputStream is = DatabaseUtil.class.getClassLoader().getResourceAsStream("schema.sql");
        if (is == null) {
            throw new RuntimeException("schema.sql not found in classpath");
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        // Split by semicolons to execute each statement
        String[] statements = sb.toString().split(";");
        try (Statement stmt = conn.createStatement()) {
            for (String sql : statements) {
                String trimmed = sql.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    stmt.execute(trimmed);
                }
            }
        }
    }

    private static void seedData(Connection conn) throws SQLException {
        // Check if admin user exists
        try (Statement stmt = conn.createStatement()) {
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM users WHERE role = 'ADMIN'");
            if (rs.next() && rs.getInt(1) == 0) {
                // Create default admin user with password "admin123"
                String hash = org.mindrot.jbcrypt.BCrypt.hashpw("admin123", org.mindrot.jbcrypt.BCrypt.gensalt());
                try (var pstmt = conn.prepareStatement(
                        "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, ?)")) {
                    pstmt.setString(1, "admin");
                    pstmt.setString(2, "admin@loosenotes.local");
                    pstmt.setString(3, hash);
                    pstmt.setString(4, "ADMIN");
                    pstmt.executeUpdate();
                }
                LOGGER.info("Default admin user created: admin / admin123");
            }
        }
    }
}
