package com.loosenotes.listener;

import com.loosenotes.util.DatabaseUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(AppListener.class);
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("Initializing Loose Notes application...");
        
        try {
            DatabaseUtil.initializeDatabase();
            logger.info("Database initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize database", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Loose Notes application shutting down...");
    }
}
