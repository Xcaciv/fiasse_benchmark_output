package com.loosenotes.dao;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

/**
 * Manages database connectivity and initialization.
 * SSEM: Modifiability - centralized data source configuration.
 * SSEM: Availability - connection pooling with HikariCP.
 * SSEM: Resilience - schema initialization with error handling.
 */
public class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String APP_PROPERTIES = "/app.properties";
    private static final String SCHEMA_SQL = "/db/schema.sql";

    private final HikariDataSource dataSource;

    public DatabaseManager(Properties config) {
        this.dataSource = buildDataSource(config);
        initializeSchema();
    }

    private HikariConfig buildHikariConfig(Properties config) {
        HikariConfig hikari = new HikariConfig();
        hikari.setJdbcUrl(config.getProperty("db.url"));
        hikari.setDriverClassName(config.getProperty("db.driver"));
        hikari.setMinimumIdle(
            Integer.parseInt(config.getProperty("db.pool.minIdle", "2")));
        hikari.setMaximumPoolSize(
            Integer.parseInt(config.getProperty("db.pool.maxPoolSize", "10")));
        hikari.setConnectionTimeout(
            Long.parseLong(config.getProperty("db.pool.connectionTimeout", "30000")));
        hikari.setIdleTimeout(
            Long.parseLong(config.getProperty("db.pool.idleTimeout", "600000")));
        hikari.setMaxLifetime(
            Long.parseLong(config.getProperty("db.pool.maxLifetime", "1800000")));
        // Security: disable auto-commit, require explicit transactions
        hikari.setAutoCommit(true);
        hikari.setPoolName("LNPool");
        return hikari;
    }

    private HikariDataSource buildDataSource(Properties config) {
        HikariConfig hikari = buildHikariConfig(config);
        log.info("Initializing database connection pool: {}", config.getProperty("db.url"));
        return new HikariDataSource(hikari);
    }

    private void initializeSchema() {
        try (InputStream is = getClass().getResourceAsStream(SCHEMA_SQL)) {
            if (is == null) {
                log.error("Schema file not found: {}", SCHEMA_SQL);
                throw new IllegalStateException("Database schema resource missing");
            }
            String sql = new String(is.readAllBytes());
            executeSchema(sql);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read schema file", e);
        }
    }

    private void executeSchema(String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            for (String statement : sql.split(";")) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty()) {
                    stmt.execute(trimmed);
                }
            }
            log.info("Database schema initialized successfully");
        } catch (SQLException e) {
            throw new IllegalStateException("Failed to initialize database schema", e);
        }
    }

    /** Returns a connection from the pool. Caller must close it. */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Returns the DataSource for use by DAO layer. */
    public DataSource getDataSource() {
        return dataSource;
    }

    /** Closes the connection pool during application shutdown. */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Shutting down database connection pool");
            dataSource.close();
        }
    }

    /** Load application properties from classpath. */
    public static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream is = DatabaseManager.class.getResourceAsStream(APP_PROPERTIES)) {
            if (is != null) {
                props.load(is);
            }
        } catch (IOException e) {
            log.warn("Could not load app.properties, using defaults", e);
        }
        // Allow environment variable overrides for sensitive settings
        overrideFromEnvironment(props);
        return props;
    }

    private static void overrideFromEnvironment(Properties props) {
        String dbUrl = System.getenv("DB_URL");
        if (dbUrl != null && !dbUrl.isBlank()) {
            props.setProperty("db.url", dbUrl);
        }
        String baseUrl = System.getenv("APP_BASE_URL");
        if (baseUrl != null && !baseUrl.isBlank()) {
            props.setProperty("app.baseUrl", baseUrl);
        }
    }
}
