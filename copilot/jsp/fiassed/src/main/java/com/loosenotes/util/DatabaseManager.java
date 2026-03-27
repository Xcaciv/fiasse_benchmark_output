package com.loosenotes.util;

import org.h2.jdbcx.JdbcDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Collectors;

public class DatabaseManager {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseManager.class);
    private static volatile DatabaseManager instance;
    private final JdbcDataSource dataSource;

    private DatabaseManager() {
        String dbPath = System.getProperty("user.home") + "/loosenotes/db/loosenotes";
        String url = "jdbc:h2:file:" + dbPath + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
        dataSource = new JdbcDataSource();
        dataSource.setURL(url);
        dataSource.setUser("sa");
        dataSource.setPassword("");
        logger.info("DatabaseManager initialized with URL: {}", url);
    }

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

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    public void initializeDatabase() throws SQLException, IOException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/schema.sql")) {
            if (is == null) {
                throw new IOException("schema.sql not found on classpath");
            }
            String sql = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
                    .lines().collect(Collectors.joining("\n"));
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
                for (String statement : sql.split(";")) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
            }
        }
        logger.info("Database initialized successfully");
    }
}
