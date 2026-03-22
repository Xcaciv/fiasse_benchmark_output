package com.loosenotes.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages the connection pool and schema initialization.
 * Single instance per application; closed via AppContextListener (Resilience).
 */
public final class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);

    private final HikariDataSource dataSource;

    public DatabaseManager(String dbPath) {
        HikariConfig config = buildHikariConfig(dbPath);
        this.dataSource = new HikariDataSource(config);
        initSchema();
    }

    /** Trust boundary: dbPath from context-param, not user input. */
    private HikariConfig buildHikariConfig(String dbPath) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:sqlite:" + dbPath);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.addDataSourceProperty("journal_mode", "WAL");
        config.addDataSourceProperty("foreign_keys", "true");
        return config;
    }

    /** Reads and executes schema.sql; idempotent due to IF NOT EXISTS. */
    private void initSchema() {
        try (InputStream is = getClass().getResourceAsStream("/schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("schema.sql not found on classpath");
            }
            String sql = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            try (Connection conn = dataSource.getConnection();
                 Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
            log.info("Database schema initialized");
        } catch (IOException | SQLException e) {
            log.error("Schema initialization failed", e);
            throw new IllegalStateException("Cannot initialize database", e);
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /** Called by AppContextListener on shutdown (Resilience: resource cleanup). */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("Database connection pool closed");
        }
    }
}
