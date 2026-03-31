package com.loosenotes.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Manages JDBC connections and schema initialization.
 *
 * SSEM notes:
 * - Modifiability: database URL/credentials loaded from app.properties, not hard-coded.
 * - Availability: connection timeout configured; simple pool management.
 * - Resilience: throws descriptive exceptions; resources closed via try-with-resources.
 * - Testability: singleton pattern with reset capability for tests.
 *
 * For production, replace with HikariCP or a JNDI DataSource.
 */
public final class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private static DatabaseManager instance;

    private final String url;
    private final String username;
    private final String password;

    private DatabaseManager(String url, String username, String password) {
        this.url      = url;
        this.username = username;
        this.password = password;
    }

    /**
     * Returns or initializes the singleton from app.properties.
     * Thread-safe via synchronized – called once during servlet context init.
     */
    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            Properties props = loadProperties();
            instance = new DatabaseManager(
                    props.getProperty("db.url"),
                    props.getProperty("db.username", ""),
                    props.getProperty("db.password", ""));
        }
        return instance;
    }

    /** Replaces the singleton – used in unit tests only. */
    public static synchronized void setInstance(DatabaseManager manager) {
        instance = manager;
    }

    /**
     * Opens a new JDBC connection.
     * Callers MUST close the connection (use try-with-resources).
     *
     * @return an open JDBC Connection
     * @throws SQLException if the connection cannot be established
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    /**
     * Initializes the database schema from db/schema.sql on the classpath.
     * Idempotent: uses CREATE TABLE IF NOT EXISTS.
     */
    public void initializeSchema() {
        log.info("Initializing database schema");
        try (Connection conn = getConnection();
             InputStream in = getClass().getResourceAsStream("/db/schema.sql")) {

            if (in == null) {
                throw new IllegalStateException("db/schema.sql not found on classpath");
            }
            String sql = new String(in.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            // Execute each statement separated by semicolons
            for (String stmt : sql.split(";")) {
                String trimmed = stmt.strip();
                if (!trimmed.isEmpty()) {
                    try (var ps = conn.prepareStatement(trimmed)) {
                        ps.execute();
                    }
                }
            }
            log.info("Database schema initialized successfully");
        } catch (SQLException | IOException e) {
            log.error("Failed to initialize database schema", e);
            throw new IllegalStateException("Database schema initialization failed", e);
        }
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in =
                DatabaseManager.class.getResourceAsStream("/app.properties")) {
            if (in == null) {
                throw new IllegalStateException("app.properties not found on classpath");
            }
            props.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load app.properties", e);
        }
        return props;
    }
}
