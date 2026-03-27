package com.loosenotes.config;

import com.loosenotes.dao.*;
import com.loosenotes.service.*;
import com.loosenotes.util.RateLimiter;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application lifecycle manager.
 * Wires up services and stores them in ServletContext for DI into servlets.
 * SSEM: Modifiability - single place for wiring dependencies.
 * SSEM: Resilience - scheduled cleanup of in-memory rate limit state.
 */
@WebListener
public class AppContextListener implements ServletContextListener {

    private static final Logger log = LoggerFactory.getLogger(AppContextListener.class);

    private DatabaseManager databaseManager;
    private ScheduledExecutorService scheduler;

    @Override
    public void contextInitialized(ServletContextEvent event) {
        log.info("Initializing Loose Notes application context");
        Properties config = DatabaseManager.loadProperties();
        databaseManager = new DatabaseManager(config);

        wireServices(event.getServletContext(), config);
        startMaintenanceScheduler(event.getServletContext());
        log.info("Application context initialized successfully");
    }

    private void wireServices(ServletContext ctx, Properties config) {
        // DAOs
        UserDao userDao = new UserDao(databaseManager.getDataSource());
        NoteDao noteDao = new NoteDao(databaseManager.getDataSource());
        AttachmentDao attachmentDao = new AttachmentDao(databaseManager.getDataSource());
        RatingDao ratingDao = new RatingDao(databaseManager.getDataSource());
        ShareLinkDao shareLinkDao = new ShareLinkDao(databaseManager.getDataSource());
        AuditLogDao auditLogDao = new AuditLogDao(databaseManager.getDataSource());
        PasswordResetTokenDao tokenDao = new PasswordResetTokenDao(databaseManager.getDataSource());

        // Services
        AuditService auditService = new AuditService(auditLogDao);
        UserService userService = new UserService(userDao);
        NoteService noteService = new NoteService(noteDao, attachmentDao);
        String uploadDir = config.getProperty("file.upload.directory", "./uploads");
        AttachmentService attachmentService = new AttachmentService(attachmentDao, noteDao, uploadDir);
        RatingService ratingService = new RatingService(ratingDao, noteDao);
        ShareLinkService shareLinkService = new ShareLinkService(shareLinkDao, noteDao);
        PasswordResetService passwordResetService =
            new PasswordResetService(tokenDao, userDao, userService);

        // Rate limiter for login endpoint (SSEM: Availability)
        int maxAttempts = Integer.parseInt(config.getProperty("ratelimit.login.maxAttempts", "5"));
        long windowSecs = Long.parseLong(config.getProperty("ratelimit.login.windowSeconds", "900"));
        RateLimiter loginRateLimiter = new RateLimiter(maxAttempts, windowSecs);

        // Store in context for servlet access
        ctx.setAttribute("appConfig",          config);
        ctx.setAttribute("userService",         userService);
        ctx.setAttribute("noteService",         noteService);
        ctx.setAttribute("attachmentService",   attachmentService);
        ctx.setAttribute("ratingService",       ratingService);
        ctx.setAttribute("shareLinkService",    shareLinkService);
        ctx.setAttribute("auditService",        auditService);
        ctx.setAttribute("passwordResetService", passwordResetService);
        ctx.setAttribute("loginRateLimiter",    loginRateLimiter);
    }

    private void startMaintenanceScheduler(ServletContext ctx) {
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "maintenance-scheduler");
            t.setDaemon(true);
            return t;
        });
        RateLimiter limiter = (RateLimiter) ctx.getAttribute("loginRateLimiter");
        // Evict expired rate limit entries every 10 minutes
        scheduler.scheduleAtFixedRate(limiter::evictExpired, 10, 10, TimeUnit.MINUTES);
    }

    @Override
    public void contextDestroyed(ServletContextEvent event) {
        log.info("Shutting down application context");
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }
}
