package com.loosenotes.bootstrap;

import com.loosenotes.dao.ActivityLogDao;
import com.loosenotes.dao.UserDao;
import com.loosenotes.db.Database;
import com.loosenotes.service.EmailService;
import com.loosenotes.service.FileStorageService;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.SessionCookieConfig;
import javax.servlet.annotation.WebListener;

@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger LOGGER = Logger.getLogger(AppContextListener.class.getName());

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext context = sce.getServletContext();
        try {
            Path dataDir = resolveDataDirectory(context);
            Database.initialize(dataDir.resolve("loose-notes.db"));
            FileStorageService.initialize(dataDir.resolve("storage"));
            EmailService.initialize(dataDir.resolve("outbox"));
            seedDefaultAdmin();
            configureSessionCookie(context);
            context.setAttribute("dataDir", dataDir.toString());
            LOGGER.info(() -> "Loose Notes initialized with data directory: " + dataDir);
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "Failed to initialize application", ex);
            throw new IllegalStateException("Application startup failed", ex);
        }
    }

    private Path resolveDataDirectory(ServletContext context) {
        String configured = System.getProperty("loosenotes.dataDir");
        if (configured == null || configured.isBlank()) {
            configured = context.getInitParameter("loosenotes.dataDir");
        }
        if (configured == null || configured.isBlank()) {
            return Paths.get(System.getProperty("user.dir"), "data");
        }
        return Paths.get(configured);
    }

    private void configureSessionCookie(ServletContext context) {
        SessionCookieConfig config = context.getSessionCookieConfig();
        config.setHttpOnly(true);
        config.setName("LOOSE_NOTES_SESSION");
    }

    private void seedDefaultAdmin() throws SQLException {
        UserDao userDao = new UserDao();
        if (userDao.createInitialAdminIfMissing("admin", "admin@example.com", "ChangeMe123!")) {
            new ActivityLogDao().log(null, "system.seed_admin", "Default admin account created for first-run bootstrap.");
            LOGGER.warning("Created default admin account 'admin'. Change the password immediately.");
        }
    }
}
