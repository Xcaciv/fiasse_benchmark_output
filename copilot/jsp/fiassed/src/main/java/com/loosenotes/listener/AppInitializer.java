package com.loosenotes.listener;

import com.loosenotes.util.DatabaseManager;
import com.loosenotes.util.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

@WebListener
public class AppInitializer implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(AppInitializer.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        try {
            DatabaseManager.getInstance().initializeDatabase();
            createDefaultAdmin();
            logger.info("LoooseNotes application initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize application", e);
            throw new RuntimeException("Application initialization failed", e);
        }
    }

    private void createDefaultAdmin() throws Exception {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            String checkSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
            try (PreparedStatement ps = conn.prepareStatement(checkSql);
                 ResultSet rs = ps.executeQuery()) {
                rs.next();
                if (rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO users (username, email, password_hash, role) VALUES (?, ?, ?, 'ADMIN')";
                    try (PreparedStatement ins = conn.prepareStatement(insertSql)) {
                        ins.setString(1, "admin");
                        ins.setString(2, "admin@loosenotes.local");
                        ins.setString(3, SecurityUtils.hashPassword("Admin@123456!"));
                        ins.executeUpdate();
                    }
                    logger.info("Default admin user created (username: admin, password: Admin@123456!)");
                }
            }
        }
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("LoooseNotes application shutting down");
    }
}
