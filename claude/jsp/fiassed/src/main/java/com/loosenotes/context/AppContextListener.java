package com.loosenotes.context;

import com.loosenotes.util.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Application lifecycle listener responsible for initializing and tearing down
 * shared infrastructure resources.
 *
 * <p>On startup ({@link #contextInitialized}):
 * <ul>
 *   <li>Acquires the {@link DatabaseManager} singleton, which creates the
 *       connection pool and applies the DDL schema if it has not yet been
 *       created.</li>
 *   <li>Records a structured startup log entry so that deployment events are
 *       visible in log aggregation systems.</li>
 * </ul>
 * </p>
 *
 * <p>On shutdown ({@link #contextDestroyed}):
 * <ul>
 *   <li>Calls {@link DatabaseManager#shutdown()} to close the connection pool
 *       and release file handles, preventing resource leaks during hot-deploy
 *       cycles or container restarts.</li>
 * </ul>
 * </p>
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        String appName    = sce.getServletContext().getServletContextName();
        String serverInfo = sce.getServletContext().getServerInfo();

        log.info("Application starting: name='{}' server='{}'", appName, serverInfo);

        try {
            // Initialize the database connection pool and ensure the schema exists.
            DatabaseManager.getInstance();
            log.info("DatabaseManager initialized successfully — schema is ready");
        } catch (Exception e) {
            // Log the error but do not rethrow: the container will continue
            // deploying and individual requests will fail with meaningful errors
            // rather than the entire application failing to deploy silently.
            log.error("Failed to initialize DatabaseManager during startup", e);
        }

        log.info("Application startup complete");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        log.info("Application shutting down — releasing resources");

        try {
            DatabaseManager.getInstance().shutdown();
            log.info("DatabaseManager shut down successfully");
        } catch (Exception e) {
            // Shutdown errors are logged but not rethrown so that other
            // ServletContextListeners registered in the container still execute.
            log.error("Error shutting down DatabaseManager", e);
        }

        log.info("Application shutdown complete");
    }
}
