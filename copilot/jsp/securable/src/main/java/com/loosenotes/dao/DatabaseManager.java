package com.loosenotes.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

/**
 * Singleton database manager for SQLite.
 * Initialises the schema on first use and enforces WAL mode + foreign keys.
 * Double-checked locking ensures thread-safe singleton creation.
 */
public final class DatabaseManager {

    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static final String DB_DIR = System.getProperty("user.home") + "/.loosenotes";
    private static final String DB_URL = "jdbc:sqlite:" + DB_DIR + "/loosenotes.db";

    private static volatile DatabaseManager instance;
    private volatile boolean schemaInitialised = false;

    private DatabaseManager() {}

    /** Thread-safe singleton accessor. */
    public static DatabaseManager getInstance() {
        if (instance == null) {
            synchronized (DatabaseManager.class) {
                if (instance == null) {
                    instance = new DatabaseManager();
                }
            }
        }
        return instance;
    }

    /**
     * Returns a new JDBC connection with WAL mode and foreign-key enforcement.
     * Callers MUST close this connection (use try-with-resources).
     * Trust boundary: any SQL must use PreparedStatements with ? placeholders.
     */
    public Connection getConnection() throws SQLException {
        ensureDbDirectory();
        Connection conn = DriverManager.getConnection(DB_URL);
        configurePragmas(conn);
        if (!schemaInitialised) {
            initialiseSchema(conn);
        }
        return conn;
    }

    private void ensureDbDirectory() {
        Path dir = Paths.get(DB_DIR);
        if (!Files.exists(dir)) {
            try {
                Files.createDirectories(dir);
            } catch (Exception e) {
                log.error("Failed to create database directory: {}", DB_DIR, e);
            }
        }
    }

    private void configurePragmas(Connection conn) throws SQLException {
        try (Statement st = conn.createStatement()) {
            st.execute("PRAGMA journal_mode=WAL");
            st.execute("PRAGMA foreign_keys = ON");
        }
    }

    /** Idempotent schema initialisation — runs schema.sql from classpath once. */
    private synchronized void initialiseSchema(Connection conn) {
        if (schemaInitialised) {
            return;
        }
        try {
            String sql = loadSchemaFromClasspath();
            executeSchemaStatements(conn, sql);
            schemaInitialised = true;
            log.info("Database schema initialised successfully.");
        } catch (Exception e) {
            log.error("Failed to initialise database schema", e);
        }
    }

    private String loadSchemaFromClasspath() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            if (is == null) {
                throw new IllegalStateException("schema.sql not found on classpath");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(is, StandardCharsets.UTF_8))) {
                return reader.lines().collect(Collectors.joining("\n"));
            }
        }
    }

    private void executeSchemaStatements(Connection conn, String sql) throws SQLException {
        // Split on semicolons to execute each DDL statement individually
        String[] statements = sql.split(";");
        try (Statement st = conn.createStatement()) {
            for (String statement : statements) {
                String trimmed = statement.trim();
                if (!trimmed.isEmpty() && !trimmed.startsWith("--")) {
                    st.execute(trimmed);
                }
            }
        }
    }
}
