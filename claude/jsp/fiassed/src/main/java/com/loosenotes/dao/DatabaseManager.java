package com.loosenotes.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Singleton connection manager for H2 embedded database.
 * Initializes schema on first startup from classpath schema.sql.
 * Each caller is responsible for closing the returned Connection.
 */
public final class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static volatile DatabaseManager instance;

    private final String url;
    private final String username;
    private final String password;

    private DatabaseManager(Properties cfg) {
        this.url      = cfg.getProperty("db.url",      "jdbc:h2:file:./data/loosenotes;AUTO_SERVER=TRUE;MODE=MySQL");
        this.username = cfg.getProperty("db.username", "sa");
        this.password = cfg.getProperty("db.password", "");
        initSchema();
    }

    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager(loadConfig());
                }
            }
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public void shutdown() {
        try (Connection c = getConnection();
             Statement  s = c.createStatement()) {
            s.execute("SHUTDOWN");
            log.info("H2 database shutdown complete");
        } catch (SQLException e) {
            log.warn("Non-fatal error during H2 shutdown: {}", e.getMessage());
        }
    }

    // ------------------------------------------------------------------ private

    private static Properties loadConfig() {
        Properties p = new Properties();
        try (InputStream is = DatabaseManager.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (is != null) {
                p.load(is);
            } else {
                log.warn("db.properties not found on classpath; using defaults");
            }
        } catch (IOException e) {
            log.warn("Failed to load db.properties: {}; using defaults", e.getMessage());
        }
        return p;
    }

    private void initSchema() {
        try (InputStream is = getClass().getClassLoader()
                .getResourceAsStream("schema.sql")) {
            if (is == null) {
                log.error("schema.sql not found on classpath — database not initialized");
                return;
            }
            String ddl = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection c = getConnection();
                 Statement  s = c.createStatement()) {
                for (String stmt : ddl.split(";")) {
                    String trimmed = stmt.strip();
                    if (!trimmed.isEmpty()) {
                        s.execute(trimmed);
                    }
                }
                log.info("Database schema initialized successfully");
            }
        } catch (IOException | SQLException e) {
            log.error("Schema initialization failed: {}", e.getMessage(), e);
            throw new IllegalStateException("Cannot initialize database schema", e);
        }
    }
}
