package com.loosenotes.listener;

import com.loosenotes.util.DatabaseManager;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.util.logging.Logger;

/**
 * Initialises and tears down the database connection pool with the web application.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger LOGGER = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        LOGGER.info("Loose Notes application starting — initialising database…");
        DatabaseManager.initialize();
        LOGGER.info("Database ready.");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOGGER.info("Loose Notes application stopping — releasing database pool.");
        DatabaseManager.shutdown();
    }
}
